#!/usr/bin/env bash
# Job-started hook for the self-hosted deploy runner on kube01. The runner runs
# it before every job; a non-zero exit fails the job before any step runs.
#
# Only pushes to main of this repository may use the runner. A pull request can
# rewrite the workflow to target the runner, but it cannot change these GITHUB_*
# values - so the runner, which has cluster-admin access, never executes PR code.
#
# This is a reference copy: the runner uses /home/mina/actions-runner/hooks/
# job-started.sh, set through ACTIONS_RUNNER_HOOK_JOB_STARTED in
# /home/mina/actions-runner/.env. Editing this file in a PR changes nothing.
set -u

repo=ErisEve/BibliotekaApp-UI-API

if [[ ${GITHUB_REPOSITORY:-} == "$repo" &&
      ${GITHUB_EVENT_NAME:-} == push &&
      ${GITHUB_REF:-} == refs/heads/main &&
      ${GITHUB_WORKFLOW_REF:-} == "$repo/.github/workflows/ci.yml@refs/heads/main" ]]; then
  exit 0
fi

echo "::error::This runner only runs ci.yml for pushes to main of $repo (got event=${GITHUB_EVENT_NAME:-?} ref=${GITHUB_REF:-?} workflow=${GITHUB_WORKFLOW_REF:-?}). Refusing the job."
exit 1
