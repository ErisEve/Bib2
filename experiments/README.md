# Availability experiments

Scripts that measure how the deployed stack behaves under load while pods,
rollouts and nodes are disrupted. They are independent of the deployed commit,
so the same matrix can be run against every version. The findings are in
[REPORT.md](REPORT.md).

## Requirements

Run everything on a host with `kubectl` access to the cluster. These runs used
**kube01**, from a dedicated clone (`~/biblioteka-experiments`), not the
runner's `_work` directory. You also need:

- [k6](https://grafana.com/docs/k6/) on `PATH`;
- `jq`, `curl`, `gzip`;
- Python 3 with `pandas` and `matplotlib` for `analyze.py`. Without pip, use
  `uv venv ~/exp-venv && VIRTUAL_ENV=~/exp-venv uv pip install pandas matplotlib`.

## Scripts

| Script | What it does |
|---|---|
| `k6/load.js` | Constant-arrival-rate load: `RATE`/s reads (`/api/books/booksAll`, `/api/lendings`, `/api/seats`, round-robin, one bearer token) and 1/s `POST /api/auth/login`. 10 s timeout, only HTTP 200 counts as success, read-only. |
| `calibrate.sh [rate ...]` | Runs the load at increasing rates and prints p50/p95/p99 and the success rate, to choose `RATE`. |
| `sample-metrics.sh <out.csv>` | Every 5 s: `kubectl top pods --containers`, `kubectl top nodes` and pod phase/readiness/restarts/node, into three CSVs. |
| `reset.sh` | Uncordons the agents, restarts every app Deployment, waits for the rollouts, runs `k8s/smoke-test.sh`, then waits 60 s. |
| `run.sh <label> <scenario> [service]` | One run: 30 s idle, k6 start, 90 s warm-up, the disruption, k6 runs to the end (6 min, or 10 min for `steady`). |
| `run-matrix.sh <label>` | The whole matrix, 3 rounds, `reset.sh` before each run. Resumable. |
| `analyze.py [label ...]` | Per-run and per-cell metrics, memory tables, plots, and a comparison across labels. |

Scenarios (`run.sh`):

| Scenario | Disruption |
|---|---|
| `steady` | none |
| `update` | `kubectl rollout restart` of every app Deployment at once (a CI deploy retags all six images) |
| `kill <service>` | `kubectl delete pod -l app=<service>` |
| `crash <service>` | the same with `--grace-period=0 --force` |
| `drain` | `kubectl drain` of the worker that does **not** run `postgres-0`, 60 s, uncordon |

The matrix is `steady`, `update`, `kill` of gateway, user and library services,
`crash gateway-service` and `drain`, with 3 runs each. A series takes about
4 hours.

## Running a series

```bash
gh run list -L 3                  # (anywhere) nothing queued or in progress
k8s/deploy.sh <commit>            # the version to measure
RATE=<rate> nohup experiments/run-matrix.sh <label> > matrix-<label>.log 2>&1 &
~/exp-venv/bin/python experiments/analyze.py <label> [<label> ...]
```

Use the same `RATE`, the same load host and the same matrix for every label.
Don't push to `main` during a series, because the CI deploy would invalidate
it. `run.sh` refuses to start while the runner is executing a job, and marks a
run invalid if the deployed commit changed while it ran. `run-matrix.sh`
refuses to continue a label on a different commit than it started with.

## Results

`results/<label>/<cell>/<UTC time>/` holds one run: `events.log` (millisecond
UTC timestamps), `meta.txt`, pod listings before and after, the metrics CSVs,
`summary.json`, `per_second.csv` (written by `analyze.py`) and `DONE` once
valid. The raw `k6.csv.gz` (about 2 MB per run) is gitignored.

`analyze.py` writes `results/<label>/summary.md`, `runs.csv`, `cells.csv`,
`memory.csv`, `nodes.csv` and `plots/`, plus `results/comparison.md` and
`results/comparison/` when given several labels. The docstring at the top of
`analyze.py` defines each metric.
