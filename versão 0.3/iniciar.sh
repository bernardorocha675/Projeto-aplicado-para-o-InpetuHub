#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
source scripts/runtime.sh
use_java || die 'Execute primeiro: bash configurar.sh'
use_maven || die 'Execute primeiro: bash configurar.sh'
[[ -f .env.local && -f .tools/ready ]] || die 'Execute primeiro: bash configurar.sh'
# Executa uma cópia: no Windows e no Unix o próprio atualizador pode ser substituído.
mkdir -p .tools
update_runner=$(mktemp .tools/Atualizador-XXXXXX.java)
cp scripts/Atualizador.java "$update_runner"
update_status=0
"$JAVA_HOME/bin/java" -Dfile.encoding=UTF-8 --source 17 "$update_runner" || update_status=$?
rm -f "$update_runner"
if [[ "$update_status" == 10 ]]; then
  # Releia o novo comando (inclusive o novo nome do JAR), sem consultar novamente.
  exec env INPETU_AUTO_UPDATE=0 bash iniciar.sh "$@"
fi
[[ "$update_status" == 0 ]] || die 'Atualizacao interrompida. Consulte ATUALIZACOES.md.'
# Recompila para que alteracoes no codigo aparecam na proxima inicializacao.
"$MAVEN_CMD" -B -ntp -DskipTests clean package
echo 'Iniciando InPETU. Abra http://localhost:8080 (ou a PORT de .env.local). Ctrl+C para parar.'
exec "$JAVA_HOME/bin/java" -jar target/inpetu-hub-0.3.3.jar --spring.profiles.active=local "$@"
