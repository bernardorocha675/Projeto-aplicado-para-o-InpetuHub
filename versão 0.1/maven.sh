#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
source scripts/runtime.sh
use_java || die 'Execute primeiro: bash configurar.sh'
use_maven || die 'Execute primeiro: bash configurar.sh'
exec "$MAVEN_CMD" "$@"
