"""O que muda entre Linux e Windows (decisão 11: Linux primeiro, depois Windows; sem macOS).

No Windows não há /proc nem sinais: os processos são consultados e encerrados pela API do Windows (ctypes, só
biblioteca padrão), e a passagem para o iniciador novo abre o novo processo e encerra este.
"""
import os
import pathlib
import platform as _platform
import subprocess
import sys

WINDOWS = os.name == "nt"


def nomes(nome_do_so=None):
    """(caminho do java no pacote, nome do executável do iniciador)."""
    exe = ".exe" if (nome_do_so or os.name) == "nt" else ""
    return f"runtime/bin/java{exe}", f"inpetu-hub{exe}"


JAVA, INICIADOR = nomes()


def plataforma(maquina=None, nome_do_so=None):
    """Como no nome dos pacotes: Linux-x64, Linux-ARM64, Windows-x64 ou Windows-ARM64."""
    maquina = (maquina or _platform.machine()).lower()
    sistema = "Windows" if (nome_do_so or os.name) == "nt" else "Linux"
    return f"{sistema}-{'ARM64' if maquina in ('aarch64', 'arm64') else 'x64'}"


def pasta_de_configuracao(ambiente, nome_do_so=None):
    if (nome_do_so or os.name) == "nt":
        base = ambiente.get("LOCALAPPDATA") or ambiente.get("APPDATA")
        if base:
            return pathlib.Path(base)
        return pathlib.Path(ambiente.get("USERPROFILE", "~")).expanduser() / "AppData" / "Local"
    base = ambiente.get("XDG_CONFIG_HOME")
    return pathlib.Path(base) if base else pathlib.Path(ambiente.get("HOME", "~")).expanduser() / ".config"


def trocar_processo(executavel, argumentos, nome_do_so=None, trocar=os.execv, abrir=subprocess.Popen,
                    sair=os._exit):
    comando = [str(executavel), *argumentos]
    if (nome_do_so or os.name) == "nt":
        abrir(comando, close_fds=True)
        sair(0)
    else:
        trocar(str(executavel), comando)


def opcoes_do_processo():
    """O sistema roda separado do iniciador; no Windows, sem abrir uma janela de console."""
    if WINDOWS:
        return {"creationflags": subprocess.CREATE_NO_WINDOW | subprocess.CREATE_NEW_PROCESS_GROUP}
    return {"start_new_session": True}


if WINDOWS:
    import ctypes
    from ctypes import wintypes

    _k32 = ctypes.WinDLL("kernel32", use_last_error=True)
    _k32.OpenProcess.restype = wintypes.HANDLE
    _k32.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
    _k32.CloseHandle.argtypes = [wintypes.HANDLE]
    _k32.GetExitCodeProcess.argtypes = [wintypes.HANDLE, ctypes.POINTER(wintypes.DWORD)]
    _k32.TerminateProcess.argtypes = [wintypes.HANDLE, wintypes.UINT]
    _CONSULTAR, _ENCERRAR, _AINDA_ATIVO = 0x1000, 0x0001, 259

    class _Texto(ctypes.Structure):
        _fields_ = [("tamanho", wintypes.USHORT), ("maximo", wintypes.USHORT), ("buffer", ctypes.c_void_p)]

    def _abrir(pid, acesso):
        return _k32.OpenProcess(acesso, False, pid)

    def vivo(pid):
        alca = _abrir(pid, _CONSULTAR)
        if not alca:
            return ctypes.get_last_error() == 5  # acesso negado: existe, mas é de outra pessoa
        try:
            codigo = wintypes.DWORD()
            return bool(_k32.GetExitCodeProcess(alca, ctypes.byref(codigo))) and codigo.value == _AINDA_ATIVO
        finally:
            _k32.CloseHandle(alca)

    def matar(pid):
        alca = _abrir(pid, _ENCERRAR)
        if alca:
            try:
                _k32.TerminateProcess(alca, 1)
            finally:
                _k32.CloseHandle(alca)

    def linha_de_comando(pid):
        """A linha de comando de outro processo (NtQueryInformationProcess, classe 60, Windows 8.1 ou mais)."""
        alca = _abrir(pid, _CONSULTAR)
        if not alca:
            return ""
        try:
            ntdll = ctypes.WinDLL("ntdll")
            tamanho = wintypes.ULONG(0)
            ntdll.NtQueryInformationProcess(wintypes.HANDLE(alca), 60, None, 0, ctypes.byref(tamanho))
            if not tamanho.value or tamanho.value > 1 << 20:
                return ""
            buffer = ctypes.create_string_buffer(tamanho.value)
            if ntdll.NtQueryInformationProcess(wintypes.HANDLE(alca), 60, buffer, tamanho, ctypes.byref(tamanho)):
                return ""
            texto = _Texto.from_buffer(buffer)
            return ctypes.wstring_at(texto.buffer, texto.tamanho // 2) if texto.buffer else ""
        finally:
            _k32.CloseHandle(alca)

    def colher(pid):
        return False
else:
    import signal

    def vivo(pid):
        try:
            os.kill(pid, 0)
        except ProcessLookupError:
            return False
        except PermissionError:
            return True
        try:
            estado = pathlib.Path(f"/proc/{pid}/stat").read_text().split(")")[-1].split()[0]
            return estado != "Z"
        except OSError:
            return True

    def matar(pid):
        try:
            os.kill(pid, signal.SIGKILL)
        except ProcessLookupError:
            pass

    def linha_de_comando(pid):
        try:
            return " ".join(p.decode("utf-8", "replace")
                            for p in pathlib.Path(f"/proc/{pid}/cmdline").read_bytes().split(b"\0"))
        except OSError:
            return ""

    def colher(pid):
        """Recolhe um processo que já saiu, se for filho deste (senão ele fica como zumbi e parece vivo)."""
        try:
            os.waitpid(pid, os.WNOHANG)
        except ChildProcessError:
            pass
        return False


def bundle_ao_lado_do_executavel():
    return pathlib.Path(sys.executable).parent / "bundle.json"
