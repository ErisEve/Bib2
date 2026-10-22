#!/usr/bin/env bash
# Finds the load the cluster handles comfortably: runs the k6 scenario at
# increasing rates and prints p95 latency and the success rate of each step.
#
#   experiments/calibrate.sh [rate ...]      (default: 10 20 30 40 60 80 100)
#
# Pick RATE for the experiments at about half the rate where p95 bends or
# errors appear. Results: experiments/results/calibration/<UTC time>/.
# Environment: STEP (2m), PAUSE (30), BASE_URL, K6 (k6).
set -euo pipefail

STEP=${STEP:-2m}
PAUSE=${PAUSE:-30}
BASE_URL=${BASE_URL:-http://10.100.50.11:8080}
K6=${K6:-k6}
here=$(cd "$(dirname "$0")" && pwd)
dir=$here/results/calibration/$(date -u +%Y%m%dT%H%M%SZ)
mkdir -p "$dir"
rates=("$@")
(( ${#rates[@]} )) || rates=(10 20 30 40 60 80 100)

kubectl -n biblioteka get configmap biblioteka-deploy -o jsonpath='{.data.commit}' > "$dir/COMMIT"
printf '%6s %10s %10s %10s %10s %9s\n' rate req/s p50_ms p95_ms p99_ms success | tee "$dir/table.txt"
for rate in "${rates[@]}"; do
  "$K6" run --quiet --no-color --summary-export "$dir/rate-$rate.json" \
    -e RATE="$rate" -e DURATION="$STEP" -e BASE_URL="$BASE_URL" "$here/k6/load.js" > "$dir/rate-$rate.log" 2>&1 || true
  jq 'del(.setup_data)' "$dir/rate-$rate.json" > "$dir/tmp.json" && mv "$dir/tmp.json" "$dir/rate-$rate.json"
  jq -r --arg r "$rate" '.metrics as $m | [$r, $m.http_reqs.rate,
      $m["http_req_duration{scenario:reads}"]["p(50)"], $m["http_req_duration{scenario:reads}"]["p(95)"],
      $m["http_req_duration{scenario:reads}"]["p(99)"], ($m.success.value * 100)]
    | "\(.[0]) \(.[1]) \(.[2]) \(.[3]) \(.[4]) \(.[5])"' "$dir/rate-$rate.json" |
    awk '{ printf "%6s %10.1f %10.0f %10.0f %10.0f %8.2f%%\n", $1, $2, $3, $4, $5, $6 }' | tee -a "$dir/table.txt"
  sleep "$PAUSE"
done
