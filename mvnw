#!/usr/bin/env sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MVN_VERSION=3.9.9
MVN_DIR="$BASE_DIR/.mvn/apache-maven-$MVN_VERSION"
MVN_CMD="$MVN_DIR/bin/mvn"
ZIP_PATH="$BASE_DIR/.mvn/apache-maven-$MVN_VERSION-bin.zip"
MVN_DIST_URL="${MVN_DIST_URL:-https://maven.aliyun.com/repository/public/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.zip}"

if [ ! -x "$MVN_CMD" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  if [ ! -f "$ZIP_PATH" ]; then
    curl -fsSL "$MVN_DIST_URL" -o "$ZIP_PATH"
  fi
  unzip -oq "$ZIP_PATH" -d "$BASE_DIR/.mvn"
fi

exec "$MVN_CMD" "$@"
