#!/usr/bin/env python3
"""Testes do hook de proteção: python3 -I .claude/hooks/test_protecao.py (na raiz do projeto)."""
import json
import os
import subprocess
import sys
import unittest
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
HOOK = RAIZ / '.claude/hooks/protecao.py'


def rodar(ferramenta, entrada, branch=None):
    env = dict(os.environ, CLAUDE_PROJECT_DIR=str(RAIZ))
    dados = json.dumps({'tool_name': ferramenta, 'tool_input': entrada, 'cwd': str(RAIZ)})
    if branch:  # simula a branch atual com um git falso que só responde a branch --show-current
        falso = RAIZ / 'aplicativo/.tools/git-falso'
        falso.mkdir(parents=True, exist_ok=True)
        script = falso / 'git'
        script.write_text('#!/bin/sh\ncase "$*" in *"branch --show-current"*) echo "%s";; *) exec /usr/bin/git "$@";; esac\n' % branch)
        script.chmod(0o755)
        env['PATH'] = f'{falso}:{env["PATH"]}'
    r = subprocess.run([sys.executable, '-I', str(HOOK)], input=dados, capture_output=True, text=True, env=env)
    return r.returncode, r.stderr


class Protecao(unittest.TestCase):
    def bloqueia(self, *args, **kw):
        codigo, erro = rodar(*args, **kw)
        self.assertEqual(codigo, 2, f'deveria bloquear: {args} {kw}')
        self.assertIn('Bloqueado', erro)

    def libera(self, *args, **kw):
        codigo, erro = rodar(*args, **kw)
        self.assertEqual(codigo, 0, f'deveria liberar: {args} {kw} — {erro}')

    def test_migrations(self):
        self.bloqueia('Edit', {'file_path': str(RAIZ / 'aplicativo/src/main/resources/db/migration/V1__modelo_fisico.sql')})
        self.bloqueia('Write', {'file_path': 'aplicativo/src/main/resources/db/migration/V5__espacos_do_hub.sql'})
        self.libera('Write', {'file_path': 'aplicativo/src/main/resources/db/migration/V99__nova_aditiva.sql'})
        self.bloqueia('Bash', {'command': 'sed -i s/a/b/ aplicativo/src/main/resources/db/migration/V2__personas_precos_configuracao.sql'})
        self.libera('Bash', {'command': 'cat aplicativo/src/main/resources/db/migration/V1__modelo_fisico.sql'})

    def test_areas_protegidas(self):
        for caminho in ['Testes/README.md', 'Testes/teste99/log.txt', '.env', 'aplicativo/.env.local',
                        'aplicativo/.secrets/accounts.properties', str(RAIZ / '.secrets/mfa.key')]:
            self.bloqueia('Write', {'file_path': caminho})
        self.libera('Edit', {'file_path': 'aplicativo/src/main/java/br/com/inpetu/hub/InpetuApplication.java'})
        self.libera('Write', {'file_path': '/tmp/fora-do-projeto/.env'})
        self.bloqueia('Bash', {'command': 'rm -rf Testes/teste01'})
        self.bloqueia('Bash', {'command': 'echo x > aplicativo/.env.local'})
        self.bloqueia('Bash', {'command': 'cp chave aplicativo/.secrets/mfa.key'})
        self.libera('Bash', {'command': 'ls Testes/ && cat Testes/README.md'})
        self.libera('Bash', {'command': "python3 - <<'EOF' > saida.txt\nimport os\nprint(os.environ.get('HOME'))\nEOF"})
        self.libera('Bash', {'command': 'node --test .github/scripts/test_admin_ui.cjs > /tmp/saida.txt'})
        # Citar uma área protegida numa mensagem não é escrever nela.
        self.libera('Bash', {'command': "git commit -q -F - <<'EOF'\nescrita em Testes/, .secrets/ e .env*\nEOF\ngit push -q 2>&1 | tail -1"})
        self.libera('Bash', {'command': 'git commit -m "protege Testes/ e .env" && git push 2>&1'}, branch='fix/algo')
        self.bloqueia('Bash', {'command': 'git rm -r Testes/teste01'})
        self.bloqueia('Bash', {'command': 'cat x | tee -a .env'})
        self.bloqueia('Bash', {'command': "cat > Testes/novo.txt <<'EOF'\nconteúdo\nEOF"})
        self.bloqueia('Bash', {'command': 'FOO=1 mv a.txt aplicativo/.secrets/'})

    def test_main(self):
        self.bloqueia('Bash', {'command': 'git commit -m "x"'}, branch='main')
        self.libera('Bash', {'command': 'git commit -m "x"'}, branch='fix/algo')
        self.bloqueia('Bash', {'command': 'git push origin main'}, branch='fix/algo')
        self.bloqueia('Bash', {'command': 'git push -f origin HEAD:main'}, branch='fix/algo')
        self.bloqueia('Bash', {'command': 'git push'}, branch='main')
        self.libera('Bash', {'command': 'git push -u origin fix/algo'}, branch='fix/algo')
        self.libera('Bash', {'command': 'git push'}, branch='fix/algo')
        self.libera('Bash', {'command': 'git log origin/main --oneline -5'}, branch='main')

    def test_falha_do_hook_libera(self):
        r = subprocess.run([sys.executable, '-I', str(HOOK)], input='isto não é json', capture_output=True, text=True)
        self.assertEqual(r.returncode, 0)
        self.assertIn('Aviso', r.stderr)


if __name__ == '__main__':
    unittest.main(verbosity=1)
