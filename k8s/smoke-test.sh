#!/usr/bin/env bash
# Post-deploy smoke test: exercises every route the gateway serves, through the
# Ingress, the way a browser does. Read-only - nothing is written to the database.
#
#   k8s/smoke-test.sh [base-url]
#
# The checks are retried until they all pass or SMOKE_TIMEOUT runs out, because
# the gateway answers with fallbacks (503) while the services behind it are
# still starting.
#
# Environment:
#   SMOKE_BASE_URL   default http://10.100.50.11:8080 (any node works)
#   SMOKE_EMAIL      a user that exists in the database (default: seeded test@example.com)
#   SMOKE_PASSWORD   default 123 (every seeded user's password)
#   SMOKE_TIMEOUT    seconds to keep retrying, default 240
set -uo pipefail

BASE_URL="${1:-${SMOKE_BASE_URL:-http://10.100.50.11:8080}}"
BASE_URL="${BASE_URL%/}"
EMAIL="${SMOKE_EMAIL:-test@example.com}"
PASSWORD="${SMOKE_PASSWORD:-123}"
TIMEOUT="${SMOKE_TIMEOUT:-240}"

body=$(mktemp)
trap 'rm -f "$body"' EXIT

# request <method> <path> [curl args...] - sets $code, leaves the body in $body.
request() {
  local method=$1 path=$2
  shift 2
  code=$(curl -sS -o "$body" -w '%{http_code}' --max-time 20 -X "$method" "$@" "$BASE_URL$path" 2>/dev/null) || code=000
}

# check_json_array <path> - GET with the token, expect 200 and a JSON array.
check_json_array() {
  request GET "$1" -H "Authorization: Bearer $token"
  if [[ $code != 200 ]] || ! jq -e 'type == "array"' "$body" >/dev/null 2>&1; then
    echo "  FAIL GET $1 -> $code $(head -c 200 "$body")"
    return 1
  fi
  echo "  ok   GET $1 ($(jq length "$body") items)"
}

run_checks() {
  local failed=0

  # ui-service via the gateway's catch-all route.
  request GET /
  if [[ $code == 200 ]] && grep -qi '<html' "$body"; then
    echo "  ok   GET /"
  else
    echo "  FAIL GET / -> $code"
    failed=1
  fi

  # user-service: login hits the database and issues a JWT.
  token=
  request POST /api/auth/login -H 'Content-Type: application/json' \
    --data "$(jq -n --arg e "$EMAIL" --arg p "$PASSWORD" '{email: $e, password: $p}')"
  if [[ $code == 200 ]]; then
    token=$(jq -r '.token // empty' "$body" 2>/dev/null)
  fi
  if [[ -z $token ]]; then
    echo "  FAIL POST /api/auth/login as $EMAIL -> $code $(head -c 200 "$body")"
    return 1
  fi
  echo "  ok   POST /api/auth/login"

  # The other three services each validate the token locally (shared JWT_SECRET)
  # and read their table.
  check_json_array /api/books/booksAll || failed=1
  check_json_array /api/lendings || failed=1
  check_json_array /api/seats || failed=1

  return $failed
}

echo "Smoke testing $BASE_URL (retrying for up to ${TIMEOUT}s)"
deadline=$((SECONDS + TIMEOUT))
attempt=1
while true; do
  echo "attempt $attempt:"
  if run_checks; then
    echo "Smoke test passed."
    exit 0
  fi
  if (( SECONDS >= deadline )); then
    echo "Smoke test FAILED after ${TIMEOUT}s." >&2
    exit 1
  fi
  attempt=$((attempt + 1))
  sleep 10
done
