#!/usr/bin/env bash
# Roda a verificação local completa do InPETU Hub e imprime um resumo para o ESTADO-ATUAL.md.
# O Java roda com clean: classes e relatórios de outra branch no target/ contariam testes que não existem aqui.
# Uso (na raiz do projeto): .claude/skills/verificar-tudo/scripts/verificar-tudo.sh [--rapido]
#   --rapido  pula o Testar.java (o roteiro de integração, que leva alguns minutos)
# Usa as ferramentas de aplicativo/.tools (jdk-21, node-24, go, apache-maven-*) quando existem;
# senão, as do sistema. Etapa sem ferramenta aparece como "não rodado". Sai com 1 se algo falhar.

raiz="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../../../.." && pwd)}"
cd "$raiz" || { echo "Não encontrei a raiz do projeto."; exit 1; }
rapido=0; [ "${1:-}" = "--rapido" ] && rapido=1

tools="$raiz/aplicativo/.tools"
for d in "$tools/jdk-21/bin" "$tools/node-24/bin" "$tools/go/bin" "$tools"/apache-maven-*/bin; do
  [ -d "$d" ] && PATH="$d:$PATH"
done
[ -d "$tools/jdk-21" ] && export JAVA_HOME="$tools/jdk-21"
export PATH

log="$(mktemp -d)"; trap 'rm -rf "$log"' EXIT
resumo=(); falhas=0
tem() { command -v "$1" >/dev/null 2>&1; }
# Python 3 de verdade: python3, python ou py -3 (no Windows, "python" pode ser o atalho da Microsoft Store).
py=()
for candidato in "python3" "python" "py -3"; do
  set -- $candidato
  tem "$1" || continue
  if "$@" -c 'import sys; sys.exit(0 if sys.version_info[0] == 3 else 1)' </dev/null >/dev/null 2>&1; then py=("$@"); break; fi
done
registra() { resumo+=("$1"); }
falhou() { resumo+=("$1"); falhas=$((falhas + 1)); }

echo "== Java (mvn test)"
if tem mvn && tem java; then
  if mvn -B -ntp -q -f aplicativo/pom.xml clean test >"$log/java.txt" 2>&1; then ok=1; else ok=0; fi
  n=$(grep -hoE 'Tests run: [0-9]+' aplicativo/target/surefire-reports/*.txt 2>/dev/null | awk '{s+=$3} END {print s+0}')
  [ "$ok" = 1 ] && registra "$n Java" || { falhou "Java FALHOU ($n rodados)"; grep -E 'FAIL|ERROR\]' "$log/java.txt" | head -20; }
else registra "Java não rodado (sem java/mvn)"; fi

echo "== Testar.java"
if [ "$rapido" = 1 ]; then registra "Testar não rodado (--rapido)"
elif tem java && tem mvn; then
  if (cd aplicativo && java "-Dfile.encoding=UTF-8" --source 17 scripts/Testar.java) >"$log/testar.txt" 2>&1; then
    registra "Testar sem falhas"
  else falhou "Testar FALHOU"; tail -20 "$log/testar.txt"; fi
else registra "Testar não rodado (sem java/mvn)"; fi

echo "== Node (interface e receptor)"
if tem node; then
  for alvo in ".github/scripts/test_admin_ui.cjs:cadastros" ".github/scripts/test_agenda_ui.cjs:agenda" "receptor/test.mjs:receptor"; do
    arq="${alvo%%:*}"; nome="${alvo##*:}"
    node --test "$arq" >"$log/node.txt" 2>&1; st=$?
    n=$(grep -oE '^ℹ tests [0-9]+' "$log/node.txt" | grep -oE '[0-9]+$')
    [ "$st" = 0 ] && registra "Node $nome ${n:-?}" || { falhou "Node $nome FALHOU"; grep -E '^✖|not ok' "$log/node.txt" | head -10; }
  done
  bad=""; for f in aplicativo/src/main/resources/static/js/*.js; do node --check "$f" 2>/dev/null || bad="$bad $f"; done
  [ -z "$bad" ] && registra "node --check ok" || falhou "node --check FALHOU:$bad"
else registra "Node não rodado (sem node)"; fi

echo "== Go (iniciador)"
if tem go; then
  fora=$(gofmt -l iniciador)
  [ -z "$fora" ] && registra "gofmt ok" || falhou "gofmt FALHOU: $fora"
  (cd iniciador && go vet ./...) >"$log/vet.txt" 2>&1 && registra "go vet ok" || { falhou "go vet FALHOU"; head -20 "$log/vet.txt"; }
  (cd iniciador && go test -count=1 -v ./...) >"$log/go.txt" 2>&1; st=$?
  n=$(grep -cE '^--- PASS' "$log/go.txt")
  [ "$st" = 0 ] && registra "$n Go" || { falhou "Go FALHOU"; grep -E '^--- FAIL|FAIL|panic' "$log/go.txt" | head -20; }
else registra "Go não rodado (sem go)"; fi

echo "== Segredos e hook de proteção"
if [ ${#py[@]} -eq 0 ]; then
  # Sem Python 3 estas etapas não rodam: contam como falha, nunca como aprovadas.
  falhou "segredos IGNORADO (Python 3 não encontrado)"
  falhou "hook de proteção IGNORADO (Python 3 não encontrado)"
else
  "${py[@]}" -I aplicativo/scripts/check_secrets.py >"$log/seg.txt" 2>&1 && registra "segredos sem problemas" || { falhou "segredos FALHOU"; cat "$log/seg.txt"; }
  "${py[@]}" -I .claude/hooks/test_protecao.py >"$log/hook.txt" 2>&1 && registra "hook de proteção ok" || { falhou "hook de proteção FALHOU"; tail -15 "$log/hook.txt"; }
fi

echo
echo "RESUMO ($(uname -s) $(uname -m), $(date +%d/%m/%Y)): $(IFS=';'; echo "${resumo[*]}" | sed 's/;/, /g')"
naorodadas=$(printf '%s\n' "${resumo[@]}" | grep -c 'não rodado')
if [ "$falhas" != 0 ]; then echo "RESULTADO: $falhas etapa(s) com falha."
elif [ "$naorodadas" != 0 ]; then echo "RESULTADO: nada falhou, mas $naorodadas etapa(s) não rodaram; não as declare como testadas."
else echo "RESULTADO: tudo aprovado."; fi
[ "$falhas" = 0 ]
