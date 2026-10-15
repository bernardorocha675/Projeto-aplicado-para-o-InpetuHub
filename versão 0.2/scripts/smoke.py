"""Teste HTTP da instalação atual sem imprimir dados do arquivo privado ou corpos de resposta."""
import http.cookiejar
import urllib.error
import os
import pathlib
import re
import subprocess
import time
import urllib.parse
import urllib.request

root = pathlib.Path(__file__).resolve().parents[1]
port = os.environ.get('SMOKE_PORT', '18080')
environment = dict(os.environ, PORT=port)
log = (root / '.tools' / 'smoke.log').open('w')
command = ['cmd.exe', '/d', '/c', 'iniciar.bat'] if os.name == 'nt' else ['bash', 'iniciar.sh']
process = subprocess.Popen(command, cwd=root, env=environment, stdout=log, stderr=subprocess.STDOUT)
base = 'http://127.0.0.1:' + port
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
try:
    for attempt in range(180):
        if process.poll() is not None:
            raise SystemExit('O servidor encerrou antes do teste HTTP; consulte .tools/smoke.log localmente.')
        try:
            with opener.open(base + '/', timeout=2) as response:
                assert response.status == 200
                assert "script-src 'self'" in response.headers['Content-Security-Policy']
            break
        except Exception:
            time.sleep(1)
    else:
        raise SystemExit('Servidor não respondeu dentro do prazo.')
    with opener.open(base + '/login') as response:
        html = response.read().decode()
    token = re.search(r'name="_csrf"[^>]*value="([^"]+)"', html)
    assert token, 'Token CSRF ausente.'
    fake = urllib.parse.urlencode({'username': 'compatibility-probe', 'password': 'ficticio', '_csrf': token[1]}).encode()
    for attempt in range(10):
        with opener.open(base + '/l%6fgin', data=fake) as response:
            assert response.url.endswith('/login?error')
    try:
        opener.open(base + '/l%6fgin', data=fake)
        raise AssertionError('Login codificado contornou o limite de tentativas.')
    except urllib.error.HTTPError as error:
        assert error.code == 429
    properties = {}
    for line in (root / '.secrets/accounts.properties').read_text().splitlines():
        if '=' in line and not line.startswith('#'):
            key, value = line.split('=', 1)
            properties[key] = value
    admin = next(key.rsplit('.', 1)[0] for key, value in properties.items() if key.endswith('.role') and value == 'ADMIN')
    data = urllib.parse.urlencode({'username': properties[admin + '.login'], 'password': properties[admin + '.password'], '_csrf': token[1]}).encode()
    with opener.open(base + '/login', data=data) as response:
        assert response.status == 200
        assert response.url.endswith(('/conta/mfa', '/admin')), 'Login não chegou ao destino interno: ' + urllib.parse.urlparse(response.url).path + ('?error' if 'error' in urllib.parse.urlparse(response.url).query else '')
    with opener.open(base + '/%61dmin/config') as response:
        assert response.url.endswith('/conta/mfa'), 'Rota codificada contornou o segundo fator.'
    with opener.open(base + '/disponibilidade') as response:
        assert response.status == 200
    assert (root / 'data/inpetu-v02.mv.db').is_file()
    java = root / '.tools/jdk/bin' / ('java.exe' if os.name == 'nt' else 'java')
    if not java.is_file():
        import shutil
        java = pathlib.Path(shutil.which('java'))
    destination = root / '.tools/smoke-lock.inpetu'
    locked = subprocess.run([str(java), '--source', '17', 'scripts/Backup.java', 'backup', str(destination)], cwd=root,
                            input='senha-ficticia-teste-lock-2026\n', text=True, capture_output=True, timeout=90)
    assert locked.returncode != 0 and not destination.exists(), 'Backup aceitou banco em execução.'
    print('Instalação, agenda, login protegido, API e H2: aprovados. Credenciais não foram impressas.')
finally:
    if os.name == 'nt':
        subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    else:
        # iniciar.sh usa exec para o Java; encerrar pelo PID real registrado por Popen.
        process.terminate()
    try:
        process.wait(timeout=30)
    except subprocess.TimeoutExpired:
        process.kill()
    log.close()
