#!/usr/bin/env bash
set -euo pipefail

root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
for service in user-service catalog-service enrollment-service; do
  (
    cd "$root/$service"
    ./mvnw test
  )
done
(
  cd "$root/gateway-service"
  ../user-service/mvnw -f pom.xml test
)
