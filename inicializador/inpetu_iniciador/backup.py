"""Cópia do banco antes de cada início (o sistema está parado: a trava garante). Guarda as 10 mais novas."""
import re
import shutil

PADRAO = re.compile(r"inpetu-\d{8}-\d{6}(-\d+)?\.mv\.db")


def copiar(pastas, agora, manter=10):
    """Devolve o caminho da cópia, ou None se ainda não há banco."""
    banco = pastas.dados / "inpetu.mv.db"
    if not banco.exists():
        return None
    pastas.backups.mkdir(parents=True, exist_ok=True)
    nome = f"inpetu-{agora:%Y%m%d-%H%M%S}"
    destino = pastas.backups / f"{nome}.mv.db"
    numero = 2
    while destino.exists():
        destino = pastas.backups / f"{nome}-{numero}.mv.db"
        numero += 1
    shutil.copy2(banco, destino)
    copias = sorted(p for p in pastas.backups.iterdir() if PADRAO.fullmatch(p.name))
    for antiga in copias[:-manter]:
        antiga.unlink()
    return destino
