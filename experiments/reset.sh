#!/usr/bin/env bash
# Puts the cluster back into a known starting state before an experiment run:
# every agent uncordoned, every app Deployment freshly restarted (pods don't move
# back after an uncordon on their own, and a restart also clears the gateway's
# circuit-breaker state), all rollouts finished, the smoke test passing, then
# SETTLE seconds of quiet.
#
#   experiments/reset.sh
#
# Environment: NAMESPACE (biblioteka), SETTLE (60), plus SMOKE_* (k8s/smoke-test.sh).
set -euo pipefail

NAMESPACE=${NAMESPACE:-biblioteka}
SETTLE=${SETTLE:-60}
SELECTOR=app.kubernetes.io/part-of=biblioteka
repo=$(cd "$(dirname "$0")/.." && pwd)

kc() { kubectl -n "$NAMESPACE" "$@"; }

echo "reset: uncordoning agents"
for node in $(kubectl get nodes -l node-role.kubernetes.io/worker=worker -o name); do
  kubectl uncordon "$node"
done

deployments=$(kc get deployment -l "$SELECTOR" -o name)
echo "reset: restarting" $deployments
kc rollout restart $deployments
for d in $deployments; do
  kc rollout status "$d" --timeout=600s
done

# rollout status returns once the new pods are ready; wait for the old ones to go too.
for _ in $(seq 90); do
  terminating=$(kc get pods -o json | jq '[.items[] | select(.metadata.deletionTimestamp)] | length')
  (( terminating == 0 )) && break
  sleep 2
done

SMOKE_TIMEOUT=${SMOKE_TIMEOUT:-300} "$repo/k8s/smoke-test.sh"

kc get pods -o wide
echo "reset: settling for ${SETTLE}s"
sleep "$SETTLE"
