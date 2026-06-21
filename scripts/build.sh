#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT_DIR"

mvn -q -s backend/settings.xml -Dmaven.repo.local=.m2/repository -f backend/pom.xml -Dmaven.test.skip=true package

echo "build 完成: backend/target/chuanzi-restaurant-assistant.jar"
