#!/usr/bin/env bash
set -euo pipefail

curl --fail --silent --show-error --max-time 10 \
  -o /dev/null "${NACOS_CONSOLE_URL:-http://localhost:18080}/"
printf 'Nacos console reachable; verify registration and business calls separately.\n'

for url in "${USER_URL:-http://localhost:8081}" "${CATALOG_URL:-http://localhost:8082}" "${ENROLLMENT_URL:-http://localhost:8083}"; do
  curl --fail --silent --show-error --max-time 10 "$url/actuator/health"
  printf '\n'
done
