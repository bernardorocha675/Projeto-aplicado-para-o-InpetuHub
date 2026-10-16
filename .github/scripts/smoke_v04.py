"""Testa o executavel e o Java embarcado com dados descartaveis; nao usa contas privadas."""
import http.cookiejar
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request

root = Path(__file__).resolve().parents[2]
project = root / 'versão 0.4'
package = json.loads((project / 'dist/smoke.json').read_text())
accounts = [('milena','milena040'),('bernardo','bernardo040'),('joaof','joaof040'),
            ('joaoe','joaoe040'),('guilherme','guilherme040')]

def wait_for(action, timeout=180):
    end = time.monotonic() + timeout
    last = None
    while time.monotonic() < end:
        try:
            value = action()
            if value:
                return value
        except (OSError, ValueError, urllib.error.URLError, AssertionError) as error:
            last = type(error).__name__
        time.sleep(.5)
    raise AssertionError('Resultado nao confirmado no prazo: ' + str(last))

def browser():
    return urllib.request.build_opener(urllib.request.ProxyHandler({}),
                                       urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def get(opener, url):
    return opener.open(url, timeout=10)
def csrf(opener, base, page='/login'):
    with get(opener, base+page) as response:
        body = response.read().decode()
    match = re.search(r'name="_csrf"[^>]*value="([^"]+)"', body)
    if not match:
        match = re.search(r'value="([^"]+)"[^>]*name="_csrf"', body)
    assert match, 'Formulario sem token'
    return match.group(1)
def post(opener, url, values):
    return opener.open(urllib.request.Request(url, data=urllib.parse.urlencode(values).encode()), timeout=15)

with tempfile.TemporaryDirectory(prefix='inpetu-desktop-smoke-') as temp:
    home = Path(temp) / 'dados com espacos'
    process = subprocess.Popen([package['executable'], '--headless', '--offline', '--start', '--home', str(home)],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        def connection():
            return json.loads((home/'running.json').read_text())['URL']
        control = wait_for(connection)
        parsed = urllib.parse.urlsplit(control)
        control_base = urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, '', '', ''))
        token = parsed.fragment
        def api(action=None):
            path = '/api/'+(action or 'status')
            req = urllib.request.Request(control_base+path, headers={'Authorization':'Bearer '+token},
                                         method='POST' if action else 'GET')
            with browser().open(req, timeout=10) as response:
                return json.loads(response.read())
        status = wait_for(lambda: (s if (s:=api())['Running'] and not s['Busy'] else None))
        assert status['Ready']
        base = status['AppURL']
        # A pagina sem token nao controla o instalador.
        try:
            browser().open(control_base+'/api/status', timeout=5)
            raise AssertionError('Controlador aceitou chamada sem token')
        except urllib.error.HTTPError as error:
            assert error.code == 403
        admin = None
        for index, (login,password) in enumerate(accounts):
            user = browser()
            with post(user, base+'/login', {'username':login,'password':password,'_csrf':csrf(user,base)}) as response:
                assert urllib.parse.urlsplit(response.url).path == '/admin'
            try:
                with get(user, base+'/admin/config') as response:
                    assert index==1 and response.status==200
            except urllib.error.HTTPError as error:
                assert index!=1 and error.code==403
            if index==1:
                admin=user
        assert not (home/'.secrets/accounts.properties').exists(), 'Arquivo de senhas nao deve ser necessario'
        fields={'nomeEspaco':'Sala persistente desktop','capacidade':'10','ativo':'on','disponibilidade':'on',
                '_csrf':csrf(admin,base,'/admin/cadastros/espacos/editar')}
        with post(admin,base+'/admin/cadastros/espacos/salvar',fields) as response:
            response.read()
        with get(admin,base+'/admin/cadastros/espacos') as response:
            assert 'Sala persistente desktop' in response.read().decode()
        before=(home/'.secrets/mfa.key').read_bytes()
        api('stop')
        wait_for(lambda: not api()['Running'] and not api()['Busy'])
        assert (home/'data/inpetu-v02.mv.db').is_file()
        api('start')
        state=wait_for(lambda: (s if (s:=api())['Running'] and not s['Busy'] else None))
        base=state['AppURL']
        for login,password in accounts:
            user=browser()
            with post(user,base+'/login',{'username':login,'password':password,'_csrf':csrf(user,base)}) as response:
                assert urllib.parse.urlsplit(response.url).path=='/admin'
            if login=='bernardo':
                admin=user
        with get(admin,base+'/admin/cadastros/espacos') as response:
            assert 'Sala persistente desktop' in response.read().decode()
        assert (home/'.secrets/mfa.key').read_bytes()==before
        api('close')
        process.wait(timeout=40)
        assert process.returncode==0
        print('EXECUTAVEL APROVADO: configuracao sem arquivo de senhas, 10 logins, permissoes, cadastro, reinicio, chave e banco preservados, encerramento.')
    finally:
        if process.poll() is None:
            try:
                api('close')
                process.wait(timeout=30)
            except Exception:
                process.kill()
                process.wait(timeout=10)
