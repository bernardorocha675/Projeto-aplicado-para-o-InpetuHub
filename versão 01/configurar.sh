#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
source scripts/runtime.sh
mkdir -p .tools data
command -v curl >/dev/null || die 'curl nao encontrado. Instale curl pelo gerenciador do sistema e repita.'
command -v tar >/dev/null || die 'tar nao encontrado. Instale tar pelo gerenciador do sistema e repita.'
if ! use_java; then
  case "$(uname -s)" in
    Darwin) platform=mac ;;
    Linux) platform=linux ;;
    *) die 'Use configurar.bat no Windows. Este script aceita macOS e Linux.' ;;
  esac
  case "$(uname -m)" in
    x86_64|amd64) architecture=x64 ;;
    aarch64|arm64) architecture=aarch64 ;;
    *) die 'Arquitetura nao suportada automaticamente: use um JDK 17 ou 21.' ;;
  esac
  echo 'Baixando JDK 17 (Temurin) para .tools, sem alterar o sistema...'
  url="https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=$architecture&image_type=jdk&os=$platform&vendor=eclipse"
  download "$url" .tools/jdk-metadata.json
  binary_url=$(sed -n 's/^[[:space:]]*"link"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' .tools/jdk-metadata.json | head -n 1)
  checksum_url=$(sed -n 's/^[[:space:]]*"checksum_link"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' .tools/jdk-metadata.json | head -n 1)
  [[ "$binary_url" == https://* && "$checksum_url" == https://* ]] || die 'A API nao retornou um JDK compativel.'
  download "$binary_url" .tools/jdk.tar.gz
  download "$checksum_url" .tools/jdk.sha256
  verify_archive .tools/jdk.tar.gz .tools/jdk.sha256 256
  rm -rf .tools/jdk-stage
  mkdir -p .tools/jdk-stage
  tar -xzf .tools/jdk.tar.gz --strip-components=1 -C .tools/jdk-stage
  rm -rf .tools/jdk
  mv .tools/jdk-stage .tools/jdk
  rm -f .tools/jdk.tar.gz .tools/jdk.sha256 .tools/jdk-metadata.json
  use_java || die 'Nao foi possivel executar o JDK baixado nesta maquina.'
fi
echo "Java: $JAVA_HOME"
if ! use_maven; then
  echo "Baixando Maven $MAVEN_VERSION para .tools..."
  url="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  download "$url" .tools/maven.tar.gz
  download "$url.sha512" .tools/maven.sha512
  verify_archive .tools/maven.tar.gz .tools/maven.sha512 512
  rm -rf .tools/maven-stage
  mkdir -p .tools/maven-stage
  tar -xzf .tools/maven.tar.gz -C .tools/maven-stage
  rm -rf ".tools/apache-maven-$MAVEN_VERSION"
  mv ".tools/maven-stage/apache-maven-$MAVEN_VERSION" .tools/
  rmdir .tools/maven-stage
  rm -f .tools/maven.tar.gz .tools/maven.sha512
  use_maven || die 'Falha ao preparar Maven.'
fi
if [[ ! -f .env.local ]]; then
  (umask 077; cp .env.local.example .env.local)
fi
echo 'Baixando dependencias, compilando e executando os testes...'
rm -f .tools/ready
"$MAVEN_CMD" -B -ntp clean verify
touch .tools/ready
echo 'Configuracao concluida. Para iniciar: bash iniciar.sh'
echo 'O banco em data/ sera criado automaticamente na primeira inicializacao.'
