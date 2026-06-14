#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SENDER_ID="${SENDER_ID:-1}"
RECIPIENT_ID="${RECIPIENT_ID:-2}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-100}"
CONCURRENCY="${CONCURRENCY:-10}"
TTL_SECONDS="${TTL_SECONDS:-3600}"

echo "ThreadBridge Secure load test"
echo "BASE_URL=$BASE_URL"
echo "TOTAL_REQUESTS=$TOTAL_REQUESTS"
echo "CONCURRENCY=$CONCURRENCY"

send_request() {
  local index="$1"
  local request_id="load-test-$(date +%s)-${index}"

  curl -sS -o /tmp/threadbridge-response-"$index".json \
    -w "%{http_code}" \
    -X POST "$BASE_URL/webhook/secure-messages" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: $SENDER_ID" \
    -d "{
      \"requestId\": \"$request_id\",
      \"recipientId\": $RECIPIENT_ID,
      \"text\": \"load test message $index\",
      \"ttlSeconds\": $TTL_SECONDS,
      \"oneTime\": true
    }"
}

export -f send_request
export BASE_URL SENDER_ID RECIPIENT_ID TTL_SECONDS

seq 1 "$TOTAL_REQUESTS" | xargs -n 1 -P "$CONCURRENCY" bash -c 'send_request "$@"' _

echo
echo "Load test finished"