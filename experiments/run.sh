#!/usr/bin/env bash
# One experiment run: load from k6 plus (optionally) one disruption.
#
#   experiments/run.sh <label> <scenario> [service]
#
#   steady            no disruption (STEADY_DURATION, 10m)
#   update            kubectl rollout restart of every app Deployment at once -
#                     what a CI deploy does, since every push retags all six images
#   kill  <service>   kubectl delete pod -l app=<service> (graceful) - every pod
#                     of the service, so with two replicas both go at once
#   crash <service>   the same with --grace-period=0 --force
#   kill1 / crash1 <service>
#                     the same, but only one pod (the first by name); identical
#                     to kill/crash for a single-replica service
#   drain             drain the worker that does NOT run postgres-0, wait 60 s,
#                     uncordon
#
# Results go to experiments/results/<label>/<scenario>[-<service>]/<UTC time>/:
#   events.log        millisecond UTC timestamps of everything the script did
#   meta.txt          deployed commit, images, RATE, durations, ...
#   pods-before.txt, pods-after.txt
#   metrics.csv, metrics-pods.csv, metrics-nodes.csv   (sample-metrics.sh)
#   k6.csv.gz, summary.json, k6.log
#   DONE              written last, only if the run is valid
#
# Timeline: sampler starts, IDLE s without load, k6 starts, WARMUP s of load,
# `start` + disruption, `end`, k6 runs to DISRUPT_DURATION (6m) and exits.
#
# Environment: RATE (20), BASE_URL (http://10.100.50.11:8080), IDLE (30),
# WARMUP (90), STEADY_DURATION (10m), DISRUPT_DURATION (6m), NAMESPACE, K6 (k6).
set -euo pipefail

usage() { echo "usage: $0 <label> <steady|update|kill|crash|kill1|crash1|drain> [service]" >&2; exit 2; }
label=${1:-}; scenario=${2:-}; service=${3:-}
[[ -n $label ]] || usage
case $scenario in
  steady | update | drain) [[ -z $service ]] || usage ;;
  kill | crash | kill1 | crash1) [[ -n $service ]] || usage ;;
  *) usage ;;
esac

NAMESPACE=${NAMESPACE:-biblioteka}
RATE=${RATE:-20}
BASE_URL=${BASE_URL:-http://10.100.50.11:8080}
IDLE=${IDLE:-30}
WARMUP=${WARMUP:-90}
K6=${K6:-k6}
SELECTOR=app.kubernetes.io/part-of=biblioteka
if [[ $scenario == steady ]]; then duration=${STEADY_DURATION:-10m}; else duration=${DISRUPT_DURATION:-6m}; fi

here=$(cd "$(dirname "$0")" && pwd)
cell=$scenario${service:+-$service}
dir=$here/results/$label/$cell/$(date -u +%Y%m%dT%H%M%SZ)

kc() { kubectl -n "$NAMESPACE" "$@"; }
now() { date -u +%FT%T.%3NZ; }
epoch() { date +%s.%N; }
since() { awk -v a="$1" -v b="$(epoch)" 'BEGIN { printf "%.1f", b - a }'; }
log() { echo "$(now) $*" | tee -a "$dir/events.log"; }
deployed_commit() { kc get configmap biblioteka-deploy -o jsonpath='{.data.commit}'; }

# A CI deploy during the run would invalidate it (and must never be interrupted).
if pgrep -f Runner.Worker >/dev/null; then
  echo "run: a GitHub Actions job is running on this runner; not starting" >&2
  exit 3
fi

mkdir -p "$dir"
commit=$(deployed_commit)
{
  echo "label=$label"
  echo "scenario=$scenario"
  echo "service=$service"
  echo "commit=$commit"
  echo "rate=$RATE"
  echo "duration=$duration"
  echo "idle=$IDLE"
  echo "warmup=$WARMUP"
  echo "base_url=$BASE_URL"
  echo "load_host=$(hostname)"
  echo "k6=$("$K6" version | head -1)"
  kc get deployment -l "$SELECTOR" -o jsonpath='{range .items[*]}image.{.metadata.name}={.spec.template.spec.containers[0].image}{"\n"}{end}'
  kc get deployment -l "$SELECTOR" -o jsonpath='{range .items[*]}replicas.{.metadata.name}={.spec.replicas}{"\n"}{end}'
} > "$dir/meta.txt"
kc get pods -o wide > "$dir/pods-before.txt"

sampler_pid= k6_pid= cordoned=
cleanup() {
  if [[ -n $cordoned ]]; then kubectl uncordon "$cordoned" || true; fi
  if [[ -n $k6_pid ]]; then kill "$k6_pid" 2>/dev/null || true; fi
  if [[ -n $sampler_pid ]]; then kill "$sampler_pid" 2>/dev/null || true; fi
}
trap cleanup EXIT

# app_state <app>: "<ready non-terminating pods> <terminating pods> <desired replicas>"
app_state() {
  local desired
  desired=$(kc get deployment "$1" -o jsonpath='{.spec.replicas}')
  kc get pods -l app="$1" -o json | jq -r --arg d "$desired" '
    [.items[] | select(.metadata.deletionTimestamp | not)
              | select([.status.containerStatuses[]?.ready] | (length > 0 and all))] as $ready
    | [.items[] | select(.metadata.deletionTimestamp)] as $term
    | "\($ready | length) \($term | length) \($d)"'
}

# wait_replaced <app> <t0>: logs `ready` once the desired number of new pods is
# ready and `terminated` once the old ones are gone.
wait_replaced() {
  local app=$1 t0=$2 ready=0 gone=0 r t d
  for _ in $(seq 1200); do
    read -r r t d < <(app_state "$app") || true
    if [[ -z $d ]]; then sleep 0.5; continue; fi
    if (( !ready && r >= d )); then log "ready $app $(since "$t0")s"; ready=1; fi
    if (( !gone && t == 0 )); then log "terminated $app $(since "$t0")s"; gone=1; fi
    (( ready && gone )) && return 0
    sleep 0.5
  done
  log "timeout waiting for $app"
  return 1
}

wait_all_ready() {
  local d
  for d in $(kc get deployment -l "$SELECTOR" -o name); do
    kc rollout status "$d" --timeout=600s >/dev/null
  done
}

disrupt() {
  local t0 d pg target rc
  case $scenario in
    steady) ;;
    update)
      t0=$(epoch)
      kc rollout restart $(kc get deployment -l "$SELECTOR" -o name)
      # Watch all rollouts in parallel; a plain `wait` would also wait for k6 and the sampler.
      local -a watchers=()
      for d in $(kc get deployment -l "$SELECTOR" -o name); do
        (kc rollout status "$d" --timeout=600s >/dev/null && log "rollout-done ${d#*/} $(since "$t0")s") &
        watchers+=($!)
      done
      rc=0
      for d in "${watchers[@]}"; do wait "$d" || rc=1; done
      log "rollout-duration $(since "$t0")s"
      return $rc
      ;;
    kill | crash | kill1 | crash1)
      local -a force=() pods=(-l app="$service")
      [[ $scenario == crash* ]] && force=(--grace-period=0 --force)
      if [[ $scenario == *1 ]]; then
        pods=("$(kc get pod -l app="$service" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' | sort | head -1)")
      fi
      t0=$(epoch)
      kc delete pod "${pods[@]}" --wait=false "${force[@]}" || return 1
      log "deleted $service"
      wait_replaced "$service" "$t0"
      ;;
    drain)
      pg=$(kc get pod postgres-0 -o jsonpath='{.spec.nodeName}')
      target=$(kubectl get nodes -l node-role.kubernetes.io/worker=worker -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' |
        grep -vxF "$pg" | head -1)
      if [[ -z $pg || -z $target || $target == "$pg" ]]; then
        log "drain: refusing (postgres-0 on '$pg', target '$target')"
        return 1
      fi
      log "drain-begin $target (postgres-0 on $pg)"
      t0=$(epoch)
      cordoned=$target
      rc=0
      kubectl drain "$target" --ignore-daemonsets --delete-emptydir-data --timeout=5m || rc=$?
      log "drain-done $target rc=$rc $(since "$t0")s"
      sleep 60
      kubectl uncordon "$target"
      cordoned=
      log "uncordon $target"
      wait_all_ready
      log "all-ready $(since "$t0")s"
      ;;
  esac
}

log "sampler-start"
"$here/sample-metrics.sh" "$dir/metrics.csv" &
sampler_pid=$!
sleep "$IDLE"

log "k6-launch rate=$RATE duration=$duration"
K6_CSV_TIME_FORMAT=unix_milli "$K6" run --quiet --no-color \
  --out "csv=$dir/k6.csv" --summary-export "$dir/summary.json" \
  -e RATE="$RATE" -e DURATION="$duration" -e BASE_URL="$BASE_URL" \
  "$here/k6/load.js" > "$dir/k6.log" 2>&1 &
k6_pid=$!
sleep "$WARMUP"

log "start $scenario $service"
disrupt_rc=0
disrupt || disrupt_rc=$?
log "end rc=$disrupt_rc"

k6_rc=0
wait "$k6_pid" || k6_rc=$?
k6_pid=
log "k6-exit rc=$k6_rc"
sleep 5
kill "$sampler_pid" 2>/dev/null || true
sampler_pid=

kc get pods -o wide > "$dir/pods-after.txt"
gzip -f "$dir/k6.csv"
# setup_data holds the bearer token; keep it out of the committed results.
if [[ -s $dir/summary.json ]]; then
  jq 'del(.setup_data)' "$dir/summary.json" > "$dir/summary.json.tmp" && mv "$dir/summary.json.tmp" "$dir/summary.json"
fi

after=$(deployed_commit)
if [[ $after != "$commit" ]]; then
  log "INVALID: the deployed commit changed during the run ($commit -> $after)"
  exit 1
fi
if (( k6_rc != 0 || disrupt_rc != 0 )); then
  log "INVALID: k6 rc=$k6_rc, disruption rc=$disrupt_rc"
  exit 1
fi
touch "$dir/DONE"
echo "$dir"
