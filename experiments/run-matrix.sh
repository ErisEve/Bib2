#!/usr/bin/env bash
# The full experiment matrix for one label, REPS repetitions per cell, with
# experiments/reset.sh before every run:
#
#   experiments/run-matrix.sh <label>
#
#   steady | update | kill gateway-service | kill user-service |
#   kill library-management-service | crash gateway-service | drain
#
# Resumable: a cell's completed runs are the subdirectories with a DONE file,
# and cells that already have REPS of them are skipped. Repetitions are
# interleaved (round 1 of every cell, then round 2, ...) so slow drift over the
# hours a series takes doesn't land on one cell.
#
# The first run records the deployed commit in results/<label>/COMMIT; later
# runs refuse to start if a different commit is deployed.
#
# Environment: REPS (3), plus everything run.sh and reset.sh read (RATE!).
set -euo pipefail

label=${1:?usage: $0 <label>}
REPS=${REPS:-3}
NAMESPACE=${NAMESPACE:-biblioteka}
here=$(cd "$(dirname "$0")" && pwd)
results=$here/results/$label

cells=(
  "steady"
  "update"
  "kill gateway-service"
  "kill user-service"
  "kill library-management-service"
  "crash gateway-service"
  "drain"
)

done_runs() { find "$results/$1" -mindepth 2 -maxdepth 2 -name DONE 2>/dev/null | wc -l; }

mkdir -p "$results"
commit=$(kubectl -n "$NAMESPACE" get configmap biblioteka-deploy -o jsonpath='{.data.commit}')
if [[ -s $results/COMMIT ]]; then
  if [[ $(cat "$results/COMMIT") != "$commit" ]]; then
    echo "run-matrix: $label was measured on $(cat "$results/COMMIT"), but $commit is deployed" >&2
    exit 1
  fi
else
  echo "$commit" > "$results/COMMIT"
fi

for round in $(seq "$REPS"); do
  for c in "${cells[@]}"; do
    read -r scenario service <<< "$c"
    cell=$scenario${service:+-$service}
    if (( $(done_runs "$cell") >= round )); then continue; fi
    echo "=== $(date -u +%FT%TZ) $label $cell, round $round"
    "$here/reset.sh"
    "$here/run.sh" "$label" "$scenario" $service ||
      echo "=== run failed: $label $cell (will be retried on the next invocation)" >&2
  done
done

for c in "${cells[@]}"; do
  read -r scenario service <<< "$c"
  cell=$scenario${service:+-$service}
  echo "$cell: $(done_runs "$cell")/$REPS"
done
