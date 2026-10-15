#!/usr/bin/env bash
# Compartilhado pelos scripts, executados na raiz do projeto.
MAVEN_VERSION=3.9.9
die() { echo "ERRO: $*" >&2; exit 1; }

valid_jdk() {
  [[ -x "$1/bin/java" && -x "$1/bin/javac" ]] || return 1
  local version
  version=$("$1/bin/javac" -version 2>&1) || return 1
  [[ "$version" =~ ^javac\ (17|21)(\.|$) ]]
}

use_java() {
  local candidate discovered
  for candidate in "$PWD/.tools/jdk/Contents/Home" "$PWD/.tools/jdk" "${JAVA_HOME:-}"; do
    if [[ -n "$candidate" ]] && valid_jdk "$candidate"; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      return 0
    fi
  done
  if command -v java >/dev/null 2>&1; then
    discovered=$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.home = //p')
    if [[ -n "$discovered" ]] && valid_jdk "$discovered"; then
      export JAVA_HOME="$discovered"
      export PATH="$JAVA_HOME/bin:$PATH"
      return 0
    fi
  fi
  return 1
}

use_maven() {
  MAVEN_CMD="$PWD/.tools/apache-maven-$MAVEN_VERSION/bin/mvn"
  [[ -x "$MAVEN_CMD" ]]
}

download() {
  curl --fail --location --retry 3 --connect-timeout 20 --max-time 900 "$1" -o "$2.part" || {
    rm -f "$2.part"
    die "Falha no download. Confira internet/proxy e repita configurar: $1"
  }
  mv "$2.part" "$2"
}

verify_archive() {
  local actual expected
  expected=$(awk '{print $1; exit}' "$2" | tr '[:upper:]' '[:lower:]')
  if command -v shasum >/dev/null 2>&1; then
    actual=$(shasum -a "$3" "$1" | awk '{print $1}')
  elif command -v "sha${3}sum" >/dev/null 2>&1; then
    actual=$("sha${3}sum" "$1" | awk '{print $1}')
  else
    die 'Instale shasum ou sha256sum/sha512sum para verificar os downloads.'
  fi
  [[ -n "$expected" && "$actual" == "$expected" ]] || die "Checksum invalido: $1. Repita a configuracao."
}
