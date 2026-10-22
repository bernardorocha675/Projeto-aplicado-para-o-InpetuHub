#!/usr/bin/env bash
# Hook SessionStart: atualiza o repositório e mostra o contexto da sessão.
# Nunca falha: qualquer erro vira aviso e o script continua.

cd "${CLAUDE_PROJECT_DIR:-$(dirname "$0")/../..}" 2>/dev/null || { echo "Aviso: não foi possível entrar na pasta do projeto."; exit 0; }

aviso() { echo "Aviso: $*"; }

echo "== Repositório =="
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  aviso "esta pasta não é um repositório Git."
else
  if ! timeout 30 git fetch --prune origin >/dev/null 2>&1; then
    aviso "git fetch falhou (sem internet ou sem acesso ao GitHub). Usando dados locais."
  fi

  branch=$(git branch --show-current 2>/dev/null)
  if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
    aviso "há mudanças locais; o pull não foi feito e nada foi alterado."
  elif [ -z "$branch" ]; then
    aviso "HEAD destacado; o pull não foi feito."
  elif ! git rev-parse --abbrev-ref '@{u}' >/dev/null 2>&1; then
    aviso "a branch '$branch' não acompanha uma branch remota; o pull não foi feito."
  elif ! timeout 30 git pull --ff-only --quiet >/dev/null 2>&1; then
    aviso "git pull --ff-only falhou (histórico divergente ou sem conexão)."
  fi

  echo "Branch atual: ${branch:-(HEAD destacado)}"
  if contagem=$(git rev-list --left-right --count 'HEAD...@{u}' 2>/dev/null); then
    read -r frente atras <<<"$contagem"
    echo "Em relação a $(git rev-parse --abbrev-ref '@{u}'): $frente à frente, $atras atrás"
  else
    aviso "sem branch remota para comparar à frente/atrás."
  fi

  echo
  echo "== git status curto =="
  git status --short --branch 2>/dev/null || aviso "git status falhou."

  echo
  echo "== Últimos 5 commits da main =="
  if git rev-parse --verify --quiet origin/main >/dev/null; then
    git log --oneline -5 origin/main
  elif git rev-parse --verify --quiet main >/dev/null; then
    git log --oneline -5 main
  else
    aviso "branch main não encontrada."
  fi
fi

echo
echo "== Versão =="
if command -v gh >/dev/null 2>&1; then
  timeout 20 gh release view --json tagName,publishedAt 2>/dev/null \
    || aviso "não foi possível consultar a release mais recente (gh sem login ou sem internet)."
else
  aviso "gh não está instalado; release mais recente não consultada."
fi
if [ -f VERSION ]; then
  echo "VERSION: $(cat VERSION)"
else
  aviso "arquivo VERSION não encontrado."
fi

echo
echo "== Ferramentas =="
if command -v java >/dev/null 2>&1; then
  java -version 2>&1 | head -1
else
  aviso "java não está instalado."
fi
if command -v go >/dev/null 2>&1; then
  go version 2>/dev/null
else
  aviso "go não está instalado."
fi
if command -v node >/dev/null 2>&1; then
  echo "node $(node --version 2>/dev/null)"
else
  aviso "node não está instalado."
fi

exit 0
