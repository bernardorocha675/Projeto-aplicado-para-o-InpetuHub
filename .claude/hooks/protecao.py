#!/usr/bin/env python3
"""Hook PreToolUse: bloqueia mudanças em áreas protegidas do InPETU Hub.

- migrations já publicadas na origin/main (V*.sql existentes); migrations novas são permitidas;
- commit na main e push para a main;
- escrita ou exclusão em Testes/, .secrets/ e arquivos .env*.

Saída 2 bloqueia a ferramenta e mostra o motivo ao Claude. Qualquer falha do próprio hook
deixa a ação passar (saída 0) com um aviso, para não travar o trabalho.
"""
import json
import os
import re
import shlex
import subprocess
import sys
from pathlib import Path

MIGRATIONS = 'aplicativo/src/main/resources/db/migration/'
ESCRITA = {'Edit', 'Write', 'MultiEdit', 'NotebookEdit'}


def bloquear(motivo):
    print(f'Bloqueado pelo hook de proteção: {motivo}', file=sys.stderr)
    sys.exit(2)


def git(raiz, *args):
    r = subprocess.run(['git', '-C', str(raiz), *args], capture_output=True, text=True, timeout=10)
    return r.returncode, r.stdout.strip()


def relativo(raiz, caminho):
    p = Path(caminho)
    if not p.is_absolute():
        p = raiz / p
    try:
        return p.resolve().relative_to(raiz.resolve()).as_posix()
    except ValueError:
        return None  # fora do projeto: não é área protegida deste repositório


def area_protegida(rel):
    nome = rel.rsplit('/', 1)[-1]
    if rel == 'Testes' or rel.startswith('Testes/'):
        return 'Testes/ recebe só relatórios sanitizados publicados pela CI'
    if '/.secrets/' in f'/{rel}' or rel.endswith('/.secrets') or rel == '.secrets':
        return '.secrets/ guarda chaves e contas locais'
    if nome.startswith('.env'):
        return 'arquivos .env* guardam configuração privada'
    return None


def migration_publicada(raiz, rel):
    if not rel.startswith(MIGRATIONS) or not re.fullmatch(r'V\d+__.+\.sql', rel.rsplit('/', 1)[-1]):
        return False
    codigo, _ = git(raiz, 'cat-file', '-e', f'origin/main:{rel}')
    return codigo == 0


def checar_arquivo(raiz, caminho):
    rel = relativo(raiz, caminho)
    if rel is None:
        return
    motivo = area_protegida(rel)
    if motivo:
        bloquear(f'{rel} — {motivo}.')
    if migration_publicada(raiz, rel):
        bloquear(f'{rel} já está publicada na origin/main; o Flyway confere o checksum e o banco dos '
                 'usuários deixaria de iniciar. Crie uma migration nova (aditiva).')


# Opções globais do git que vêm antes do subcomando e levam um valor separado (git -C <dir> push).
GIT_GLOBAIS_COM_VALOR = {'-C', '-c', '--git-dir', '--work-tree', '--namespace', '--config-env', '--super-prefix', '--list-cmds'}
# Opções do push que levam um valor separado; o valor não é o remoto nem um refspec.
PUSH_COM_VALOR = {'-o', '--push-option', '--repo', '--receive-pack', '--exec'}


def analisar_git(partes):
    """De ['git', ...] devolve (pasta do -C ou None, subcomando, argumentos do subcomando)."""
    pasta, i = None, 1
    while i < len(partes) and partes[i].startswith('-'):
        opcao = partes[i]
        if opcao in GIT_GLOBAIS_COM_VALOR:
            if opcao == '-C' and i + 1 < len(partes):
                pasta = partes[i + 1] if pasta is None else str(Path(pasta) / partes[i + 1])
            i += 2
        else:  # --git-dir=x, --work-tree=x, --no-pager, -P, -p, --bare...
            i += 1
    if i >= len(partes):
        return pasta, None, []
    return pasta, partes[i], partes[i + 1:]


def refspecs_do_push(args):
    posicionais, i = [], 0
    while i < len(args):
        a = args[i]
        if a in PUSH_COM_VALOR:
            i += 2
            continue
        if not a.startswith('-'):
            posicionais.append(a)
        i += 1
    return posicionais[1:]  # o primeiro é o remoto


def checar_bash(raiz, comando):
    for trecho in re.split(r'&&|\|\||;|\|', comando):
        try:
            partes = shlex.split(trecho)
        except ValueError:
            partes = trecho.split()
        if len(partes) < 2 or partes[0].rsplit('/', 1)[-1] not in ('git', 'git.exe'):
            continue
        pasta, sub, args = analisar_git(partes)
        if sub not in ('commit', 'push'):
            continue
        alvo = raiz / pasta if pasta and not Path(pasta).is_absolute() else Path(pasta or raiz)
        _, branch = git(alvo, 'branch', '--show-current')
        if sub == 'commit' and branch == 'main':
            bloquear('commit direto na main. Crie uma branch a partir da origin/main.')
        if sub == 'push':
            if any(a in ('--mirror', '--all') for a in args):
                bloquear('push com --mirror/--all também envia a main. Envie só a sua branch.')
            refspecs = refspecs_do_push(args)
            if any(r.lstrip('+').split(':')[-1] in ('main', 'refs/heads/main') for r in refspecs):
                bloquear('push para a main. Envie a branch e abra um PR.')
            if not refspecs and branch == 'main':
                bloquear('push estando na main. Envie uma branch própria e abra um PR.')
    # Escrita ou exclusão por shell: só os destinos reais contam (alvo de > e >>, argumentos de
    # comandos que gravam ou apagam). Textos de mensagens e corpos de heredoc não são destinos.
    for alvo in destinos_de_escrita(comando):
        rel = relativo(raiz, alvo)
        if rel is None:
            continue
        motivo = area_protegida(rel)
        if motivo:
            bloquear(f'comando que grava ou apaga em {rel} — {motivo}.')
        if migration_publicada(raiz, rel):
            bloquear(f'comando que altera a migration publicada {rel}.')


GRAVAM = {'rm', 'rmdir', 'mv', 'cp', 'tee', 'truncate', 'install', 'ln', 'chmod', 'chown', 'touch', 'shred', 'unlink'}


def destinos_de_escrita(comando):
    sem_heredoc = re.sub(r"<<-?\s*(['\"]?)(\w+)\1([^\n]*)\n.*?\n\s*\2\s*(?=\n|$)", r'\3', comando, flags=re.S)
    destinos = [d for d in re.findall(r'(?<![<\d&])\d?>>?\|?\s*([^\s;&|)<>]+)', sem_heredoc)
                if not d.startswith('&') and d != '/dev/null']
    for trecho in re.split(r'&&|\|\||;|\||\n', sem_heredoc):
        try:
            partes = shlex.split(trecho)
        except ValueError:
            partes = trecho.split()
        while partes and re.fullmatch(r'\w+=.*|sudo|env|command|xargs', partes[0]):
            partes = partes[1:]
        if not partes:
            continue
        nome = partes[0].rsplit('/', 1)[-1]
        args = [p for p in partes[1:] if not p.startswith('-')]
        if nome in GRAVAM:
            destinos += args
        elif nome == 'sed' and any(re.fullmatch(r'-[a-zA-Z]*i\S*|--in-place\S*', p) for p in partes[1:]):
            destinos += args[1:]
        elif nome == 'dd':
            destinos += [p[3:] for p in partes[1:] if p.startswith('of=')]
        elif nome in ('git', 'git.exe'):
            pasta, sub, resto = analisar_git(partes)
            if sub in ('rm', 'mv'):
                destinos += [str(Path(pasta) / a) if pasta else a for a in resto if not a.startswith('-')]
    return destinos


def main():
    try:
        dados = json.load(sys.stdin)
        raiz = Path(os.environ.get('CLAUDE_PROJECT_DIR') or dados.get('cwd') or '.')
        ferramenta = dados.get('tool_name', '')
        entrada = dados.get('tool_input') or {}
        if ferramenta in ESCRITA:
            checar_arquivo(raiz, entrada.get('file_path') or entrada.get('notebook_path') or '')
        elif ferramenta == 'Bash':
            checar_bash(raiz, entrada.get('command') or '')
    except SystemExit:
        raise
    except Exception as erro:  # o hook nunca trava o trabalho por falha própria
        print(f'Aviso: o hook de proteção falhou e deixou passar ({type(erro).__name__}).', file=sys.stderr)
    sys.exit(0)


if __name__ == '__main__':
    main()
