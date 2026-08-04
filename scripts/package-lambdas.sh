#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

mvn -q -DskipTests package

mkdir -p target/lambda-pkg
cp target/todolist-project-1.0-SNAPSHOT.jar target/lambda-pkg/

cd target/lambda-pkg
zip -r ../lambda-package.zip todolist-project-1.0-SNAPSHOT.jar
cp ../lambda-package.zip ../lambda-list-package.zip
cp ../lambda-package.zip ../lambda-update-package.zip

echo "Created:"
echo "- target/lambda-package.zip"
echo "- target/lambda-list-package.zip"
echo "- target/lambda-update-package.zip"
