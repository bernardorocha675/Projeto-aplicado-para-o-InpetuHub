#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
source scripts/runtime.sh
use_java || die 'Execute primeiro: bash configurar.sh'
use_maven || die 'Execute primeiro: bash configurar.sh'
[[ -f .env.local && -f .tools/ready ]] || die 'Execute primeiro: bash configurar.sh'
# Recompila para que alteracoes no codigo aparecam na proxima inicializacao.
"$MAVEN_CMD" -B -ntp -DskipTests clean package
echo 'Iniciando InPETU. Abra http://localhost:8080 (ou a PORT de .env.local). Ctrl+C para parar.'
exec "$JAVA_HOME/bin/java" -jar target/inpetu-hub-1.0.0.jar --spring.profiles.active=local "$@"
