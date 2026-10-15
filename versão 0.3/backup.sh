#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
source scripts/runtime.sh
use_java || die 'Execute primeiro: bash configurar.sh'
exec "$JAVA_HOME/bin/java" -Dfile.encoding=UTF-8 --source 17 scripts/Backup.java "$@"
