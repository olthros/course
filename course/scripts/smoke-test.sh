#!/usr/bin/env bash
set -euo pipefail
base="${GATEWAY_URL:-http://localhost:8080}"
: "${DEMO_STUDENT_PASSWORD:?set DEMO_STUDENT_PASSWORD}"
: "${DEMO_ADMIN_PASSWORD:?set DEMO_ADMIN_PASSWORD}"
command -v jq >/dev/null || { printf 'jq is required\n' >&2; exit 1; }

status="$(curl -sS -o /dev/null -w '%{http_code}' "$base/api/students")"
[[ "$status" == 401 ]] || { printf 'expected 401, got %s\n' "$status" >&2; exit 1; }
login() {
  local body
  body="$(jq -n --arg username "$1" --arg password "$2" '{username:$username,password:$password}')"
  curl -fsS -H 'Content-Type: application/json' -d "$body" "$base/api/auth/login" | jq -er '.token'
}
student_token="$(login student1 "$DEMO_STUDENT_PASSWORD")"
admin_token="$(login admin "$DEMO_ADMIN_PASSWORD")"
curl -fsS -o /dev/null -H "Authorization: Bearer $student_token" "$base/api/students"
status="$(curl -sS -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer $student_token" "$base/api/courses")"
[[ "$status" == 403 ]] || { printf 'expected 403, got %s\n' "$status" >&2; exit 1; }
curl -fsS -o /dev/null -H "Authorization: Bearer $admin_token" "$base/api/courses"
printf 'Gateway login, routing and role checks passed.\n'
