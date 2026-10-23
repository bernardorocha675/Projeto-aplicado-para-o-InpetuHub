#!/usr/bin/env bash
# Chama protecao.py com o primeiro Python 3 que funcione: python3, python ou py -3 (Windows).
# No Windows, "python" pode ser o atalho da Microsoft Store, que existe mas não roda nada; por isso
# cada candidato precisa responder que é Python 3 antes de ser usado. Sem Python 3, avisa e libera.
dir="$(cd "$(dirname "$0")" && pwd)"
for candidato in "python3" "python" "py -3"; do
  set -- $candidato
  command -v "$1" >/dev/null 2>&1 || continue
  if "$@" -c 'import sys; sys.exit(0 if sys.version_info[0] == 3 else 1)' </dev/null >/dev/null 2>&1; then
    exec "$@" -I "$dir/protecao.py"
  fi
done
echo "Aviso: proteção desativada: Python 3 não encontrado (instale o Python 3 para reativar o hook de proteção)." >&2
exit 0
