#!/usr/bin/env python3
"""Analyzes the runs recorded by experiments/run.sh.

    experiments/analyze.py [label ...]       (default: every label in results/)

Per label, writes into results/<label>/:
    runs.csv     one row per completed run (a run directory with a DONE file)
    cells.csv    per cell: mean, min and max of every metric
    memory.csv   per service: container working set while idle and under load
                 (steady runs), and the peak seen in any run
    nodes.csv    per node: kubectl top nodes and the sum of the app pods on it
    summary.md   the tables above in markdown
    plots/       error timeline per cell (one representative run), memory

and per run, next to its k6.csv.gz, per_second.csv (requests per second of send
time by outcome) - small enough to commit, unlike k6.csv.

With more than one label it also writes results/comparison.md and
results/comparison/*.png, in the order the labels were given.

Definitions (times in seconds relative to the `start` event of events.log):
    a request belongs to the second it was SENT in (k6 timestamps a sample
      when it completes, so send = timestamp - duration);
    non-200 classes: 503 (gateway fallback), 0 (timeout or connection error),
      other;
    longest error window: longest run of consecutive seconds that each contain
      at least one non-200, anywhere in the run;
    recovery: end of the last second with a non-200 at or after `start`, i.e.
      from then on every second is 100 % successful (0 when no error followed
      `start`);
    restarts: container restarts that happened during the run (max - first
      value per pod in metrics-pods.csv).
"""
import math
import re
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import pandas as pd  # noqa: E402

RESULTS = Path(__file__).resolve().parent / "results"
CELLS = [
    "steady",
    "update",
    "kill-gateway-service",
    "kill-user-service",
    "kill-library-management-service",
    "crash-gateway-service",
    "drain",
]
CLASSES = ["503", "0", "other"]
CLASS_NAMES = {"503": "503 fallback", "0": "timeout / conn. error", "other": "other non-200"}
# Reference palette, categorical slots 1-3 (validated for all pairs).
COLORS = ["#2a78d6", "#eb6834", "#1baf7a"]
INK, MUTED, GRID = "#0b0b0b", "#52514e", "#e4e3df"

plt.rcParams.update(
    {
        "figure.dpi": 110,
        "font.size": 9,
        "axes.edgecolor": MUTED,
        "axes.labelcolor": INK,
        "axes.spines.top": False,
        "axes.spines.right": False,
        "axes.grid": True,
        "axes.axisbelow": True,
        "grid.color": GRID,
        "grid.linewidth": 0.6,
        "xtick.color": MUTED,
        "ytick.color": MUTED,
        "legend.frameon": False,
    }
)


def epoch(s):
    """ISO UTC timestamp with milliseconds -> unix seconds."""
    return pd.Timestamp(s).timestamp()


# ---------------------------------------------------------------- one run


def read_events(run):
    events = []
    for line in (run / "events.log").read_text().splitlines():
        t, _, rest = line.partition(" ")
        name, _, args = rest.partition(" ")
        events.append((epoch(t), name, args))
    return events


def read_meta(run):
    meta = {}
    for line in (run / "meta.txt").read_text().splitlines():
        k, _, v = line.partition("=")
        meta[k] = v
    return meta


def first(events, name):
    return next((t for t, n, _ in events if n == name), None)


def seconds_arg(args):
    m = re.search(r"([\d.]+)s\b", args)
    return float(m.group(1)) if m else None


def read_requests(run):
    df = pd.read_csv(
        run / "k6.csv.gz",
        usecols=["metric_name", "timestamp", "metric_value", "scenario", "name", "status"],
        dtype={"scenario": "string", "name": "string"},
    )
    df = df[(df.metric_name == "http_req_duration") & df.scenario.notna() & (df.scenario != "")]
    df = df.assign(
        sent=df.timestamp / 1000 - df.metric_value / 1000,
        status=df.status.fillna(0).astype(int),
    )
    df["cls"] = "ok"
    df.loc[df.status == 503, "cls"] = "503"
    df.loc[df.status == 0, "cls"] = "0"
    df.loc[(df.status != 200) & ~df.status.isin([0, 503]), "cls"] = "other"
    return df[["sent", "metric_value", "scenario", "name", "status", "cls"]].rename(
        columns={"metric_value": "ms"}
    )


def per_second(req, t0):
    sec = ((req.sent - t0) // 1).astype(int)
    ps = pd.crosstab(sec, req.cls).reindex(columns=["ok", *CLASSES], fill_value=0)
    ps = ps.reindex(range(sec.min(), sec.max() + 1), fill_value=0)
    ps.index.name = "second"
    ps["total"] = ps.sum(axis=1)
    ps["errors"] = ps[CLASSES].sum(axis=1)
    return ps


def longest_window(flags):
    best = cur = 0
    for f in flags:
        cur = cur + 1 if f else 0
        best = max(best, cur)
    return best


def pod_restarts(run):
    f = run / "metrics-pods.csv"
    if not f.exists():
        return math.nan
    pods = pd.read_csv(f)
    if pods.empty:
        return math.nan
    g = pods.groupby("pod").restarts
    return int((g.max() - g.first()).sum())


def drained_services(run, events):
    """Apps that had a pod on the drained node when the run started."""
    target = next((a.split()[0] for _, n, a in events if n == "drain-begin"), None)
    if not target or not (run / "pods-before.txt").exists():
        return ""
    apps = []
    for line in (run / "pods-before.txt").read_text().splitlines()[1:]:
        cols = line.split()
        if len(cols) >= 7 and target in cols and "Running" in cols:
            apps.append(re.sub(r"(-[a-z0-9]{8,10})?-[a-z0-9]{5}$", "", cols[0]))
    return " ".join(sorted(set(apps)))


def analyze_run(run):
    events = read_events(run)
    meta = read_meta(run)
    t0 = first(events, "start")
    req = read_requests(run)
    ps = per_second(req, t0)
    ps.to_csv(run / "per_second.csv")

    reads = req[req.scenario == "reads"]
    logins = req[req.scenario == "logins"]
    after = ps.loc[ps.index >= 0]
    err_after = after.index[after.errors > 0]
    total = len(req)
    non200 = int((req.cls != "ok").sum())

    row = {
        "run": run.name,
        "commit": meta.get("commit", "")[:7],
        "rate": int(meta.get("rate", 0)),
        "requests": total,
        "non200": non200,
        "non200_pct": 100 * non200 / total if total else math.nan,
        **{f"n_{c}": int((req.cls == c).sum()) for c in CLASSES},
        "login_non200": int((logins.cls != "ok").sum()),
        "longest_error_window_s": longest_window(ps.errors > 0),
        "first_error_s": float(err_after[0]) if len(err_after) else math.nan,
        "recovery_s": float(err_after[-1] + 1) if len(err_after) else 0.0,
        "p50_ms": reads.ms.quantile(0.50),
        "p95_ms": reads.ms.quantile(0.95),
        "p99_ms": reads.ms.quantile(0.99),
        "max_ms": reads.ms.max(),
        "login_p95_ms": logins.ms.quantile(0.95),
        "restarts": pod_restarts(run),
    }
    for t, n, a in events:
        if n == "rollout-duration":
            row["rollout_s"] = seconds_arg(a)
        elif n == "ready":
            row["replacement_ready_s"] = seconds_arg(a)
        elif n == "terminated":
            row["old_pod_gone_s"] = seconds_arg(a)
        elif n == "drain-done":
            row["drain_s"] = seconds_arg(a)
        elif n == "all-ready":
            row["drain_all_ready_s"] = seconds_arg(a)
    if any(n == "drain-begin" for _, n, _ in events):
        row["drained_node"] = next(a.split()[0] for _, n, a in events if n == "drain-begin")
        row["services_on_drained_node"] = drained_services(run, events)
    return row, ps, events


# ---------------------------------------------------------------- memory


def read_metrics(run):
    f = run / "metrics.csv"
    pods = run / "metrics-pods.csv"
    if not f.exists() or not pods.exists():
        return None
    m = pd.read_csv(f)
    apps = pd.read_csv(pods)[["pod", "app", "node"]].drop_duplicates("pod")
    m = m.merge(apps, on="pod", how="left")
    m["t"] = m.timestamp.map(epoch)
    return m


def memory_tables(label_dir, runs):
    idle, load, peak, nodes_rows = [], [], [], []
    for run, events in runs:
        m = read_metrics(run)
        if m is None or m.empty:
            continue
        peak.append(m)
        if run.parent.name != "steady":
            continue
        launch = first(events, "k6-launch")
        warm = launch + 60  # let the JIT and connection pools settle first
        idle.append(m[m.t < launch])
        load.append(m[m.t >= warm])
        nf = run / "metrics-nodes.csv"
        if nf.exists():
            n = pd.read_csv(nf)
            n["t"] = n.timestamp.map(epoch)
            n = n[n.t >= warm]
            app_sum = m[m.t >= warm].groupby(["timestamp", "node"]).memory_mi.sum().groupby("node")
            for node, g in n.groupby("node"):
                nodes_rows.append(
                    {
                        "node": node,
                        "run": run.name,
                        "node_median_mi": g.memory_mi.median(),
                        "node_max_mi": g.memory_mi.max(),
                        "app_pods_median_mi": app_sum.median().get(node, 0.0),
                        "app_pods_max_mi": app_sum.max().get(node, 0.0),
                    }
                )
    if not peak:
        return None, None

    def stats(frames, prefix):
        if not frames:
            return pd.DataFrame()
        d = pd.concat(frames)
        g = d.groupby("app").memory_mi
        return pd.DataFrame(
            {f"{prefix}_median_mi": g.median(), f"{prefix}_p95_mi": g.quantile(0.95), f"{prefix}_max_mi": g.max()}
        )

    mem = stats(idle, "idle").join(stats(load, "load"), how="outer").join(stats(peak, "any_run"), how="outer")
    mem = mem[[c for c in mem.columns if not c.startswith("any_run_") or c.endswith("max_mi")]]
    cpu = pd.concat(load).groupby("app").cpu_m.median() if load else None
    if cpu is not None:
        mem["load_cpu_median_m"] = cpu
    nodes = pd.DataFrame(nodes_rows).groupby("node").agg(
        node_median_mi=("node_median_mi", "median"),
        node_max_mi=("node_max_mi", "max"),
        app_pods_median_mi=("app_pods_median_mi", "median"),
        app_pods_max_mi=("app_pods_max_mi", "max"),
    ) if nodes_rows else None
    return mem.round(1), nodes


def plot_memory(mem, out, title):
    if mem is None or "load_median_mi" not in mem:
        return
    mem = mem.dropna(subset=["load_median_mi"]).sort_values("load_max_mi")
    fig, ax = plt.subplots(figsize=(7, 0.45 * len(mem) + 1.2))
    y = range(len(mem))
    h = 0.36
    ax.barh([i + h / 2 + 0.02 for i in y], mem.idle_median_mi, height=h, color=COLORS[0], label="idle, median")
    ax.barh([i - h / 2 - 0.02 for i in y], mem.load_median_mi, height=h, color=COLORS[1], label="under load, median")
    ax.scatter(mem.load_max_mi, [i - h / 2 - 0.02 for i in y], marker="|", s=120, color=INK, label="under load, max")
    for i, v in enumerate(mem.load_max_mi):
        ax.annotate(f"{v:.0f}", (v, i - h / 2), xytext=(6, -3), textcoords="offset points", color=MUTED, fontsize=8)
    ax.set_yticks(list(y), mem.index)
    ax.set_xlabel("container working set (MiB)")
    ax.set_title(title, loc="left", color=INK)
    ax.grid(axis="y", visible=False)
    ax.legend(loc="lower right", fontsize=8)
    fig.tight_layout()
    fig.savefig(out)
    plt.close(fig)


# ---------------------------------------------------------------- plots

MARKERS = {
    "start": "start",
    "end": "end",
    "deleted": "deleted",
    "ready": "replacement ready",
    "terminated": "old pod gone",
    "drain-begin": "drain",
    "drain-done": "drained",
    "uncordon": "uncordon",
    "rollout-duration": "rollouts done",
}


def plot_timeline(ax, ps, events, t0, title):
    x = ps.index
    bottom = None
    for c, color in zip(CLASSES, COLORS):
        if ps[c].sum() == 0:
            continue
        ax.bar(x, ps[c], width=1.0, bottom=bottom, color=color, label=CLASS_NAMES[c], linewidth=0)
        bottom = ps[c] if bottom is None else bottom + ps[c]
    ymax = max(5, ps.total.max())
    # Markers less than 2 s apart share one line and one label.
    groups = []
    for t, n, _ in events:
        if n in MARKERS:
            if groups and t - t0 - groups[-1][0] < 2:
                groups[-1][1].append(MARKERS[n])
            else:
                groups.append((t - t0, [MARKERS[n]]))
    for rel, names in groups:
        ax.axvline(rel, color=MUTED, linewidth=0.8, linestyle=(0, (3, 2)))
        ax.annotate(" / ".join(names), (rel, ymax), rotation=90, va="top", ha="right", fontsize=7, color=MUTED,
                    xytext=(-2, 0), textcoords="offset points")
    ax.set_ylim(0, ymax * 1.02)
    ax.set_xlim(x.min(), x.max())
    ax.set_title(title, loc="left", color=INK, fontsize=9)
    ax.set_ylabel("non-200 / s")


def representative(rows):
    """The run with the median number of non-200 responses."""
    rows = sorted(rows, key=lambda r: r[0]["non200"])
    return rows[len(rows) // 2]


# ---------------------------------------------------------------- tables


def fmt(v, digits=1):
    if v is None or (isinstance(v, float) and math.isnan(v)):
        return "–"
    if isinstance(v, float) and v.is_integer() and digits <= 1:
        return f"{v:.0f}"
    return f"{v:.{digits}f}"


def mean_range(series, digits=1):
    s = series.dropna()
    if s.empty:
        return "–"
    if len(s) == 1 or s.min() == s.max():
        return fmt(float(s.mean()), digits)
    return f"{fmt(float(s.mean()), digits)} ({fmt(float(s.min()), digits)}–{fmt(float(s.max()), digits)})"


METRICS = [
    ("requests", "requests", 0),
    ("non200", "non-200", 1),
    ("non200_pct", "non-200 %", 2),
    ("n_503", "503", 1),
    ("n_0", "timeout/0", 1),
    ("n_other", "other", 1),
    ("longest_error_window_s", "longest error window s", 1),
    ("recovery_s", "recovery s", 1),
    ("p50_ms", "p50 ms", 0),
    ("p95_ms", "p95 ms", 0),
    ("p99_ms", "p99 ms", 0),
    ("max_ms", "max ms", 0),
    ("rollout_s", "rollout s", 1),
    ("replacement_ready_s", "new pod ready s", 1),
    ("old_pod_gone_s", "old pod gone s", 1),
    ("drain_s", "drain s", 1),
    ("restarts", "restarts", 1),
]


def md_table(header, rows):
    out = ["| " + " | ".join(header) + " |", "|" + "|".join("---" for _ in header) + "|"]
    out += ["| " + " | ".join(str(c) for c in r) + " |" for r in rows]
    return "\n".join(out)


def cell_table(df):
    cells = [c for c in CELLS if c in set(df.cell)] + sorted(set(df.cell) - set(CELLS))
    metrics = [(k, n, d) for k, n, d in METRICS if k in df and df[k].notna().any()]
    rows = []
    for c in cells:
        d = df[df.cell == c]
        rows.append([c, len(d)] + [mean_range(d[k], dg) for k, _, dg in metrics])
    return md_table(["cell", "runs"] + [n for _, n, _ in metrics], rows)


# ---------------------------------------------------------------- per label


def analyze_label(label):
    label_dir = RESULTS / label
    plots = label_dir / "plots"
    plots.mkdir(exist_ok=True)
    rows, by_cell, runs_events = [], {}, []
    for done in sorted(label_dir.glob("*/*/DONE")):
        run = done.parent
        try:
            row, ps, events = analyze_run(run)
        except Exception as e:  # noqa: BLE001 - one broken run shouldn't stop the rest
            print(f"{run}: skipped ({e})", file=sys.stderr)
            continue
        row["cell"] = run.parent.name
        rows.append(row)
        by_cell.setdefault(row["cell"], []).append((row, ps, events))
        runs_events.append((run, events))
    if not rows:
        print(f"{label}: no completed runs", file=sys.stderr)
        return None

    df = pd.DataFrame(rows)
    df = df[["cell"] + [c for c in df.columns if c != "cell"]]
    df.round(2).to_csv(label_dir / "runs.csv", index=False)
    num = df.select_dtypes("number").columns
    agg = df.groupby("cell")[list(num)].agg(["mean", "min", "max"])
    agg.columns = [f"{a}_{b}" for a, b in agg.columns]
    agg.round(2).to_csv(label_dir / "cells.csv")

    for cell, cell_rows in by_cell.items():
        row, ps, events = representative(cell_rows)
        fig, ax = plt.subplots(figsize=(9, 2.6))
        plot_timeline(ax, ps, events, first(events, "start"), f"{label} · {cell} · run {row['run']} "
                      f"({row['non200']} non-200 of {row['requests']})")
        ax.set_xlabel("seconds since start (request send time)")
        if any(ps[c].sum() for c in CLASSES):
            ax.legend(loc="upper left", fontsize=8)
        fig.tight_layout()
        fig.savefig(plots / f"timeline-{cell}.png")
        plt.close(fig)

    mem, nodes = memory_tables(label_dir, runs_events)
    if mem is not None:
        mem.to_csv(label_dir / "memory.csv")
        plot_memory(mem, plots / "memory.png", f"{label}: container working set, steady runs")
    if nodes is not None:
        nodes.round(1).to_csv(label_dir / "nodes.csv")

    commit = (label_dir / "COMMIT").read_text().strip() if (label_dir / "COMMIT").exists() else ""
    md = [f"# {label}", "", f"Commit `{commit}`, RATE {', '.join(map(str, sorted(df.rate.unique())))}/s.", "",
          "Mean over the completed runs, (min–max) where they differ. Latencies are for the read scenario.", "",
          cell_table(df), ""]
    if "services_on_drained_node" in df:
        d = df[df.cell == "drain"]
        md += ["## Drain placement", "", md_table(["run", "drained node", "app pods on it at start", "non-200"],
               [[r.run, r.drained_node, r.services_on_drained_node, r.non200] for r in d.itertuples()]), ""]
    if mem is not None:
        md += ["## Memory (MiB, container working set)", "",
               "idle = before k6 starts; load = from 60 s after k6 starts (steady runs); any_run = peak in any run.",
               "", _df_md(mem), ""]
    if nodes is not None:
        md += ["## Nodes under load (MiB, steady runs)", "", _df_md(nodes.round(0)), ""]
    md += ["## Plots", ""] + [f"![{c}](plots/timeline-{c}.png)" for c in CELLS if c in by_cell]
    if (plots / "memory.png").exists():
        md += ["", "![memory](plots/memory.png)"]
    (label_dir / "summary.md").write_text("\n".join(md) + "\n")
    print(f"{label}: {len(df)} runs -> {label_dir / 'summary.md'}")
    return df, by_cell, mem


def _df_md(d):
    return md_table([d.index.name or ""] + list(d.columns),
                    [[i] + [fmt(v) if isinstance(v, float) else v for v in r] for i, r in zip(d.index, d.values)])


# ---------------------------------------------------------------- comparison


def compare(results):
    out = RESULTS / "comparison"
    out.mkdir(exist_ok=True)
    labels = list(results)
    cells = [c for c in CELLS if any(c in set(results[l][0].cell) for l in labels)]
    md = ["# Comparison", "", "Labels in order: " + ", ".join(f"`{l}`" for l in labels) + ".",
          "Mean over runs, (min–max) where they differ; latencies are for the read scenario.", ""]
    for cell in cells:
        md += [f"## {cell}", ""]
        metrics = [(k, n, d) for k, n, d in METRICS
                   if any(k in results[l][0] and results[l][0][results[l][0].cell == cell][k].notna().any() for l in labels)]
        rows = []
        for l in labels:
            d = results[l][0][results[l][0].cell == cell]
            rows.append([l, len(d)] + [mean_range(d[k], dg) if k in d else "–" for k, _, dg in metrics])
        md += [md_table(["label", "runs"] + [n for _, n, _ in metrics], rows), "",
               f"![{cell}](comparison/timeline-{cell}.png)", ""]

        present = [l for l in labels if cell in results[l][1]]
        fig, axes = plt.subplots(len(present), 1, figsize=(9, 2.1 * len(present) + 0.4), sharex=True, sharey=True,
                                 squeeze=False)
        for ax, l in zip(axes[:, 0], present):
            row, ps, events = representative(results[l][1][cell])
            plot_timeline(ax, ps, events, first(events, "start"), f"{l} · run {row['run']} "
                          f"({row['non200']} non-200 of {row['requests']})")
        handles, names = [], []
        for ax in axes[:, 0]:
            for h, n in zip(*ax.get_legend_handles_labels()):
                if n not in names:
                    handles.append(h)
                    names.append(n)
        if handles:
            fig.legend(handles, names, loc="upper right", fontsize=8, ncol=3)
        axes[-1, 0].set_xlabel("seconds since start (request send time)")
        fig.suptitle(f"{cell}: non-200 responses per second, representative run", x=0.01, ha="left", color=INK)
        fig.tight_layout()
        fig.savefig(out / f"timeline-{cell}.png")
        plt.close(fig)

    # Errors per cell and label, mean with min-max whiskers.
    fig, ax = plt.subplots(figsize=(9, 3.6))
    w = 0.8 / len(labels)
    for i, (l, color) in enumerate(zip(labels, COLORS + ["#e87ba4", "#6250d6"])):
        df = results[l][0]
        g = df.groupby("cell").non200.agg(["mean", "min", "max"]).reindex(cells)
        xs = [j + (i - (len(labels) - 1) / 2) * w for j in range(len(cells))]
        ax.bar(xs, g["mean"].fillna(0), width=w * 0.92, color=color, label=l)
        ax.errorbar(xs, g["mean"].fillna(0), yerr=[(g["mean"] - g["min"]).fillna(0), (g["max"] - g["mean"]).fillna(0)],
                    fmt="none", ecolor=INK, elinewidth=0.8, capsize=2)
    ax.set_xticks(range(len(cells)), [c.replace("-service", "").replace("-management", "") for c in cells],
                  rotation=15)
    ax.set_ylabel("non-200 responses per run")
    ax.set_yscale("symlog", linthresh=10)
    ax.grid(axis="x", visible=False)
    ax.legend(fontsize=8)
    ax.set_title("Failed requests per run (mean, min–max)", loc="left", color=INK)
    fig.tight_layout()
    fig.savefig(out / "errors.png")
    plt.close(fig)

    mems = {l: results[l][2] for l in labels if results[l][2] is not None and "load_max_mi" in results[l][2]}
    if mems:
        apps = sorted(set().union(*(m.index for m in mems.values())))
        rows = [[a] + [f"{fmt(m.load_median_mi.get(a))} / {fmt(m.load_max_mi.get(a))}" if a in m.index else "–"
                       for m in mems.values()] for a in apps]
        md += ["## Memory under load (median / max MiB per pod, steady runs)", "",
               md_table(["service"] + list(mems), rows), ""]
    md.insert(5, "![errors](comparison/errors.png)\n")
    (RESULTS / "comparison.md").write_text("\n".join(md) + "\n")
    print(f"comparison -> {RESULTS / 'comparison.md'}")


def main():
    labels = sys.argv[1:] or sorted(p.name for p in RESULTS.iterdir() if p.is_dir() and p.name != "comparison")
    results = {}
    for label in labels:
        r = analyze_label(label)
        if r is not None:
            results[label] = r
    if len(results) > 1:
        compare(results)


if __name__ == "__main__":
    main()
