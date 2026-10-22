#!/usr/bin/env bash
# Samples the namespace every INTERVAL seconds until killed:
#
#   experiments/sample-metrics.sh <out.csv>
#
#   <out.csv>        timestamp,pod,container,cpu_m,memory_mi
#                    (kubectl top pods --containers: the container working set)
#   <out>-pods.csv   timestamp,pod,app,phase,ready,restarts,node
#   <out>-nodes.csv  timestamp,node,cpu_m,memory_mi (kubectl top nodes)
#
# metrics-server only refreshes every 10-15 s, so consecutive rows can repeat.
# Environment: NAMESPACE (biblioteka), INTERVAL (5).
set -uo pipefail

out=${1:?usage: $0 <out.csv>}
pods=${out%.csv}-pods.csv
nodes=${out%.csv}-nodes.csv
NAMESPACE=${NAMESPACE:-biblioteka}
INTERVAL=${INTERVAL:-5}

[[ -s $out ]] || echo timestamp,pod,container,cpu_m,memory_mi > "$out"
[[ -s $pods ]] || echo timestamp,pod,app,phase,ready,restarts,node > "$pods"
[[ -s $nodes ]] || echo timestamp,node,cpu_m,memory_mi > "$nodes"

# "123m" -> 123, "1Gi" -> 1024, "154Mi" -> 154
to_csv='{ for (i = 2; i <= NF; i++) { gsub(/m$|Mi$/, "", $i); if ($i ~ /Gi$/) { sub(/Gi$/, "", $i); $i *= 1024 } } }'

while true; do
  next=$((SECONDS + INTERVAL))
  ts=$(date -u +%FT%T.%3NZ)
  kubectl -n "$NAMESPACE" top pods --containers --no-headers 2>/dev/null |
    awk -v ts="$ts" "$to_csv"' NF >= 4 { print ts "," $1 "," $2 "," $3 "," $4 }' >> "$out"
  kubectl top nodes --no-headers 2>/dev/null |
    awk -v ts="$ts" '{ c = $2; m = $4; sub(/m$/, "", c); if (m ~ /Gi$/) { sub(/Gi$/, "", m); m *= 1024 } else sub(/Mi$/, "", m); print ts "," $1 "," c "," m }' >> "$nodes"
  # kubectl's RESTARTS column contains spaces ("1 (5m ago)"), so read the JSON.
  kubectl -n "$NAMESPACE" get pods -o json 2>/dev/null | jq -r --arg ts "$ts" '
    .items[] | [
      $ts,
      .metadata.name,
      (.metadata.labels.app // ""),
      (if .metadata.deletionTimestamp then "Terminating" else .status.phase end),
      ([.status.containerStatuses[]?.ready] | (length > 0 and all)),
      ([.status.containerStatuses[]?.restartCount] | add // 0),
      (.spec.nodeName // "")
    ] | @csv' | tr -d '"' >> "$pods"
  while (( SECONDS < next )); do sleep 0.5; done
done
