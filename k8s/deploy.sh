#!/usr/bin/env bash
# Deploys one commit to the cluster and rolls back to the last good one if it
# does not come up healthy. Run by the `deploy` job in .github/workflows/ci.yml
# on the self-hosted runner on kube01, from a checkout with full history:
#
#   k8s/deploy.sh <commit>
#
# 1. Renders k8s/ as it was at <commit>, with every image pinned to the
#    immutable sha-<short> tag CI pushed for that commit. Not :main - a
#    rollback to :main would just pull the broken build again.
# 2. Applies it and waits for every Deployment/StatefulSet rollout.
# 3. Runs k8s/smoke-test.sh against the Ingress.
# 4. On success, records <commit> in ConfigMap biblioteka-deploy. On failure,
#    re-deploys the commit recorded there (manifests, config and images) and
#    exits 1. With no recorded commit it falls back to `kubectl rollout undo`.
#
# secrets.env is gitignored, so the Secret's current contents are read back from
# the cluster instead: the first deploy still has to be done by hand
# (`kubectl apply -k k8s/`, see README).
#
# Environment: NAMESPACE (biblioteka), ROLLOUT_TIMEOUT (600s), plus the
# SMOKE_* variables of k8s/smoke-test.sh.
set -euo pipefail

NAMESPACE=${NAMESPACE:-biblioteka}
ROLLOUT_TIMEOUT=${ROLLOUT_TIMEOUT:-600s}
STATE=biblioteka-deploy
SELECTOR=app.kubernetes.io/part-of=biblioteka
here=$(cd "$(dirname "$0")" && pwd)

commit=$(git rev-parse --verify "${1:?usage: $0 <commit>}^{commit}")
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

kc() { kubectl -n "$NAMESPACE" "$@"; }

# Same format as docker/metadata-action's `type=sha` tag.
tag_of() { echo "sha-${1:0:7}"; }

summary() {
  echo "$*"
  if [[ -n ${GITHUB_STEP_SUMMARY:-} ]]; then echo "$*" >> "$GITHUB_STEP_SUMMARY"; fi
}

# render <commit>: writes k8s/ as of <commit>, images pinned, into $work/<commit>.
render() {
  local rev=$1 tag dir=$work/$1
  tag=$(tag_of "$rev")
  mkdir -p "$dir"
  git archive "$rev" k8s | tar -x -C "$dir" || return 1
  kc get secret biblioteka-secrets \
    -o go-template='{{range $k, $v := .data}}{{$k}}={{$v | base64decode}}{{"\n"}}{{end}}' \
    > "$dir/k8s/secrets.env" || return 1
  # Every image is built and tagged on every CI run, so one tag fits all seven.
  sed -i -E "s/^(\s*newTag:).*/\1 $tag/" "$dir/k8s/kustomization.yaml"

  # Guard against the sed missing an image: nothing may still point at a moving tag.
  if kubectl kustomize "$dir/k8s" | grep -E '^\s*image: ghcr\.io/' | grep -v ":$tag\$"; then
    echo "render: the images above are not pinned to $tag" >&2
    return 1
  fi
}

wait_rollouts() {
  local r
  for r in $(kc get deployment,statefulset -l "$SELECTOR" -o name); do
    kc rollout status "$r" --timeout="$ROLLOUT_TIMEOUT" || return 1
  done
}

# release <commit>: apply, wait, smoke test. Every step checks its own status,
# because errexit is off inside a function called from an `if`.
release() {
  echo "::group::Render and apply $(tag_of "$1")"
  render "$1" || return 1
  kubectl apply -k "$work/$1/k8s" || return 1
  echo "::endgroup::"
  echo "::group::Wait for rollouts"
  wait_rollouts || { echo "::endgroup::"; return 1; }
  echo "::endgroup::"
  "$here/smoke-test.sh"
}

diagnose() {
  echo "::group::Cluster state after the failed deploy"
  kc get pods -o wide || true
  kc get events --sort-by=.lastTimestamp | tail -40 || true
  echo "::endgroup::"
}

record() {
  kc create configmap "$STATE" \
    --from-literal=commit="$1" \
    --from-literal=tag="$(tag_of "$1")" \
    --from-literal=deployedAt="$(date -u +%FT%TZ)" \
    --dry-run=client -o yaml | kubectl apply -f -
}

previous=$(kc get configmap "$STATE" -o jsonpath='{.data.commit}' 2>/dev/null || true)
echo "Deploying $commit (last good: ${previous:-none recorded})"

if release "$commit"; then
  record "$commit"
  summary "Deployed \`$(tag_of "$commit")\`; rollouts finished and the smoke test passed."
  exit 0
fi

diagnose
summary "Deploy of \`$(tag_of "$commit")\` FAILED."

if [[ $previous == "$commit" ]]; then
  summary "It is already the last good commit, so there is nothing older to roll back to."
elif [[ -n $previous ]] && git cat-file -e "$previous^{commit}" 2>/dev/null; then
  summary "Rolling back to \`$(tag_of "$previous")\`."
  if release "$previous"; then
    summary "Rollback to \`$(tag_of "$previous")\` succeeded; the cluster is back on the last good version."
  else
    diagnose
    summary "Rollback to \`$(tag_of "$previous")\` FAILED too; the cluster needs attention."
  fi
else
  # No usable record, e.g. the first automated deploy. Undo restores each
  # workload's previous pod template, but not the ConfigMap.
  summary "No last good commit recorded; rolling back with \`kubectl rollout undo\`."
  for r in $(kc get deployment,statefulset -l "$SELECTOR" -o name); do
    kc rollout undo "$r" || true
  done
  if wait_rollouts && "$here/smoke-test.sh"; then
    summary "rollout undo succeeded."
  else
    diagnose
    summary "rollout undo did not bring the cluster back; it needs attention."
  fi
fi
exit 1
