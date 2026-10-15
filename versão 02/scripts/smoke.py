"""Teste HTTP da instalação atual sem imprimir dados do arquivo privado ou corpos de resposta."""
import http.cookiejar
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
    properties = {}
    for line in (root / '.secrets/accounts.properties').read_text().splitlines():
        if '=' in line and not line.startswith('#'):
            key, value = line.split('=', 1)
            properties[key] = value
    data = urllib.parse.urlencode({'username': properties['account.1.login'], 'password': properties['account.1.password'], '_csrf': token[1]}).encode()
    with opener.open(base + '/login', data=data) as response:
        assert response.status == 200
        assert response.url.endswith(('/conta/mfa', '/admin')), 'Login não chegou ao destino interno: ' + urllib.parse.urlparse(response.url).path + ('?error' if 'error' in urllib.parse.urlparse(response.url).query else '')
    with opener.open(base + '/disponibilidade') as response:
        assert response.status == 200
    assert (root / 'data/inpetu-v02.mv.db').is_file()
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
