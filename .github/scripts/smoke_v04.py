"""Testa o executavel e o Java embarcado com dados descartaveis; nao usa contas privadas."""
import http.cookiejar
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request

root = Path(__file__).resolve().parents[2]
project = root / 'aplicativo'
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

def launcher_env():
    # Sessoes graficas podem nao exportar locale; no Linux o iniciador deve funcionar com POSIX.
    env = dict(os.environ)
    if sys.platform.startswith('linux'):
        for key in ('LANG', 'LC_ALL', 'LC_CTYPE', 'LANGUAGE'):
            env.pop(key, None)
    return env

def control_api(folder):
    control = json.loads((folder/'running.json').read_text())['URL']
    parsed = urllib.parse.urlsplit(control)
    control_base = urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, '', '', ''))
    def call(action=None):
        req = urllib.request.Request(control_base+'/api/'+(action or 'status'), headers={'Authorization':'Bearer '+parsed.fragment},
                                     method='POST' if action else 'GET')
        with browser().open(req, timeout=10) as response:
            return json.loads(response.read())
    return control, call

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
    home = Path(temp) / 'dados com espaços e acentuação'
    process = subprocess.Popen([package['executable'], '--headless', '--offline', '--start', '--home', str(home)],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, env=launcher_env())
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
        # Outro nome de host apontado para 127.0.0.1 (DNS rebinding) nao acessa o aplicativo local.
        try:
            browser().open(urllib.request.Request(base+'/login',headers={'Host':'atacante.example'}),timeout=10)
            raise AssertionError('Aplicativo aceitou host externo')
        except urllib.error.HTTPError as error:
            assert error.code==400
        with get(browser(),base+'/agenda') as response:
            public_page=response.read().decode()
        public_header=public_page.split('<header',1)[1].split('</header>',1)[0]
        # Menu do site oficial (0.5.1): externos só do domínio oficial, em nova aba; início, agenda e área interna locais.
        external=re.findall(r'href="(https?://[^"]+)"([^>]*)',public_header)
        assert external and all(u.startswith('https://inpetuhub.sites.ufsc.br/') and 'target="_blank"' in rest for u,rest in external), external
        assert not any(u.rstrip('/').endswith(('/sobre','/termos-de-uso')) for u,_ in external), 'Link oficial sabidamente quebrado'
        assert all('href="'+path+'"' in public_header for path in ['/','/agenda','/admin'])
        assert '/css/institucional.css' not in public_page
        with get(browser(),base+'/disponibilidade') as response:
            fresh_calendar=json.loads(response.read())
        # Banco novo já traz os 10 espaços do Hub (0.5.3), livres e sem reservas.
        assert fresh_calendar['dias'] and all(d['estado']=='DISPONÍVEL' and len(d['espacos'])==10 for d in fresh_calendar['dias']), 'Banco novo deve trazer os 10 espacos do Hub livres'
        assert 'Sala Maker VIA · 32 cadeiras e 6 sofás' in public_page, 'Mobiliario do espaco ausente na agenda'
        requester=browser()
        try:
            post(requester,base+'/reservas/resumo',{'data':'invalid-private-date','_csrf':csrf(requester,base,'/reserva')})
            raise AssertionError('Formulario invalido foi aceito')
        except urllib.error.HTTPError as error:
            assert error.code==400
            request_id=error.headers['X-Request-ID']
            body=error.read().decode()
            assert 'Failed to convert' not in body and 'java.time' not in body
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
        for path in ['/admin','/admin/reservas','/admin/solicitacoes','/admin/cadastros/empreendedores','/admin/cadastros/espacos','/admin/cadastros/atendimentos','/admin/cadastros/visitas','/admin/config']:
            with get(admin,base+path) as response:
                page=response.read().decode()
                assert response.status==200
            header=page.split('<header',1)[1].split('</header>',1)[0]
            assert all(u.startswith('https://inpetuhub.sites.ufsc.br/') and 'target="_blank"' in rest for u,rest in re.findall(r'href="(https?://[^"]+)"([^>]*)',header)), 'Navegacao externa indevida em '+path
            assert 'href="/admin"' in header and 'aria-label="Menu interno"' in page
        assert not (home/'.secrets/accounts.properties').exists(), 'Arquivo de senhas nao deve ser necessario'
        fields={'nomeEspaco':'Sala persistente desktop','capacidade':'10','ativo':'on','disponibilidade':'on',
                '_csrf':csrf(admin,base,'/admin/cadastros/espacos/editar')}
        with post(admin,base+'/admin/cadastros/espacos/salvar',fields) as response:
            response.read()
        with get(admin,base+'/admin/cadastros/espacos') as response:
            assert 'Sala persistente desktop' in response.read().decode()
        with get(browser(),base+'/disponibilidade') as response:
            ready_calendar=json.loads(response.read())
        assert all(d['estado']=='DISPONÍVEL' and len(d['espacos'])==11 and all(e['livres'] for e in d['espacos']) for d in ready_calendar['dias']), 'Espaco cadastrado e sem reservas deve ficar livre'
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
        # Simula um fix seguinte somente no diretorio descartavel, sem publicar release falsa.
        api('stop')
        wait_for(lambda: not api()['Running'] and not api()['Busy'])
        previous_version=state['Version']
        pieces=previous_version.split('.')
        future_version='.'.join(pieces[:2]+[str(int(pieces[2])+1)])
        next_root=home/'installed'/future_version
        shutil.copytree(home/'installed'/previous_version,next_root)
        manifest=json.loads((next_root/'bundle.json').read_text())
        manifest['Version']=future_version
        (next_root/'bundle.json').write_text(json.dumps(manifest),encoding='utf-8')
        flags='-s -w -X main.buildVersion='+future_version
        if manifest['GOOS']=='windows':
            flags+=' -H=windowsgui'
        build_env=dict(os.environ,CGO_ENABLED='0',GOOS=manifest['GOOS'],GOARCH=manifest['GOARCH'])
        subprocess.run(['go','build','-trimpath','-ldflags',flags,'-o',str(next_root/manifest['Launcher']),'.'],
                       cwd=root/'iniciador',env=build_env,check=True)
        (home/'current.json').write_text(json.dumps({'Version':future_version}),encoding='utf-8')
        old_control=control
        api('start')
        process.wait(timeout=40)
        assert process.returncode==0, 'O iniciador antigo nao encerrou corretamente'
        control=wait_for(lambda: (value if (value:=connection())!=old_control else None),timeout=60)
        parsed=urllib.parse.urlsplit(control)
        control_base=urllib.parse.urlunsplit((parsed.scheme,parsed.netloc,'','',''))
        token=parsed.fragment
        handed=wait_for(lambda: (s if (s:=api())['Running'] and not s['Busy'] else None))
        assert handed['Version']==future_version
        base=handed['AppURL']
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
        wait_for(lambda: not (home/'running.json').exists(),timeout=40)
        sessions=list((home/'logs/sessoes').glob('*/relatorio.json'))
        assert len(sessions)==1, 'A troca de iniciador deve preservar uma unica sessao'
        report=json.loads(sessions[0].read_text(encoding='utf-8'))
        assert report['outcome']=='CONCLUIDO'
        assert report['launcherVersions']==[previous_version,future_version]
        assert report['appVersions']==[previous_version,future_version]
        assert all(e['launcherVersion'] in report['launcherVersions'] and e['appVersion'] in report['appVersions']+['unknown'] for e in report['events'])
        assert any(e['component']=='app' and e['event']=='HTTP' and e.get('fields',{}).get('route')=='/login' for e in report['events'])
        assert any(e['event']=='AUDIT' and e.get('fields',{}).get('target')=='espacos' for e in report['events'])
        assert any(e['event']=='VALIDATION_REJECTED' and e.get('fields',{}).get('requestId')==request_id for e in report['events']), 'Erro tratado nao chegou ao relatorio da sessao'
        public_text=sessions[0].read_text(encoding='utf-8')
        assert 'invalid-private-date' not in public_text, 'Formulario nao pode aparecer no relatorio'
        for login,password in accounts:
            assert password not in public_text, 'Senha nao pode estar no relatorio'
        assert not (sessions[0].parent/'enviado.json').exists(), 'Offline nao pode indicar envio confirmado'
        assert (sessions[0].parent/'log.txt').is_file()
        print('EXECUTAVEL APROVADO: configuracao sem arquivo de senhas, 15 logins, permissoes, agenda vazia/livre, logs de sessao e versoes, cadastro, reinicio, troca real do iniciador, chave/banco preservados e encerramento.')
    finally:
        try:
            if (home/'running.json').exists():
                latest_control=connection()
                parsed=urllib.parse.urlsplit(latest_control)
                control_base=urllib.parse.urlunsplit((parsed.scheme,parsed.netloc,'','',''))
                token=parsed.fragment
                api('close')
                wait_for(lambda: not (home/'running.json').exists(),timeout=30)
        except Exception:
            pass
        if process.poll() is None:
            process.kill()
            process.wait(timeout=10)

# Iniciador encerrado a forca deixa o Java ativo; a proxima abertura deve encerra-lo e iniciar normalmente.
with tempfile.TemporaryDirectory(prefix='inpetu-desktop-orphan-', ignore_cleanup_errors=True) as temp:
    home = Path(temp) / 'órfão com acentuação'
    args = [package['executable'], '--headless', '--offline', '--start', '--home', str(home)]
    first = subprocess.Popen(args, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, env=launcher_env())
    second = None
    try:
        abandoned, call = wait_for(lambda: control_api(home))
        wait_for(lambda: (s if (s:=call())['Running'] and not s['Busy'] else None))
        first.kill()
        first.wait(timeout=10)
        second = subprocess.Popen(args, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, env=launcher_env())
        wait_for(lambda: (value if (value:=control_api(home))[0]!=abandoned else None), timeout=60)
        _, call = control_api(home)
        recovered = wait_for(lambda: (s if (s:=call())['Running'] and not s['Busy'] else None), timeout=240)
        with get(browser(), recovered['AppURL']+'/login') as response:
            assert response.status == 200
        call('close')
        second.wait(timeout=60)
        assert second.returncode == 0, 'O iniciador recuperado nao encerrou corretamente'
        events = [json.loads(line) for path in (home/'logs/sessoes').glob('*/eventos.jsonl')
                  for line in path.read_text(encoding='utf-8').splitlines() if line.strip()]
        assert any(e['event']=='ORPHAN_APP_FOUND' for e in events), 'Processo anterior nao identificado'
        assert any(e['event']=='ORPHAN_APP_STOPPED' for e in events), 'Processo anterior nao encerrado'
        outcomes = sorted(json.loads(path.read_text(encoding='utf-8'))['outcome'] for path in (home/'logs/sessoes').glob('*/relatorio.json'))
        assert outcomes == ['CONCLUIDO', 'INTERROMPIDO'], outcomes
        print('RECUPERACAO APROVADA: iniciador encerrado a forca, Java anterior parado pelo pedido local e sistema reiniciado.')
    finally:
        for launcher in (first, second):
            if launcher and launcher.poll() is None:
                launcher.kill()
                launcher.wait(timeout=10)
        if (home/'.secrets').is_dir():
            (home/'.secrets/launcher.stop').write_text('stop\n')
            time.sleep(5)
