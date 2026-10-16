#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
source scripts/runtime.sh
[[ "$#" == 1 ]] || die 'Use: bash acessos.sh verificar OU bash acessos.sh recuperar'
case "$1" in
  verificar) access_mode=--verificar-acessos ;;
  recuperar) access_mode=--recuperar-acessos ;;
  *) die 'Use: bash acessos.sh verificar OU bash acessos.sh recuperar' ;;
esac
use_java || die 'Execute primeiro: bash configurar.sh'
use_maven || die 'Execute primeiro: bash configurar.sh'
"$MAVEN_CMD" -B -ntp -DskipTests clean package
exec "$JAVA_HOME/bin/java" -jar target/inpetu-hub-0.4.1.jar "$access_mode"
