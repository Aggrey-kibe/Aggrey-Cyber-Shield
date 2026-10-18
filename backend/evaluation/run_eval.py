"""
Run the detector over a labelled dataset and report precision, recall, F1,
false-positive / false-negative rates (with 95% intervals) overall, per
category and per operating point.

    python -m evaluation.run_eval --dataset seed
    python -m evaluation.run_eval --dataset external --split dev --show-errors 15
    python -m evaluation.run_eval --dataset external --split test --out report.json

DISCIPLINE: the external TEST split is for final reporting only. Error
examples are refused for it, so the detector cannot be tuned by peeking.
"""
import argparse
import json
import sys
from datetime import datetime, timezone

from evaluation.datasets import (
    DATA_DIR, EXTERNAL_DIR, Sample, build_external, defang, load_jsonl,
    registered_domain, split_of,
)
from evaluation.metrics import Confusion
from app.services.scanning import RULESET_VERSION
from app.services.scanning.message_analyzer import analyze_message
from app.services.scanning.url_analyzer import InvalidUrl, analyze_url

FLAG_THRESHOLD = 25   # "suspicious" or worse
HIGH_THRESHOLD = 50   # "likely_dangerous"
THRESHOLDS = (1, 15, 25, 40, 50, 75)


def score_sample(sample: Sample) -> tuple[int | None, list[str]]:
    """Return (score, finding ids); score is None when the input cannot be assessed."""
    try:
        if sample.kind == "url":
            result = analyze_url(sample.text)
        else:
            result = analyze_message(sample.text)
    except InvalidUrl:
        return None, []
    return result.score, [f.id for f in result.findings]


def evaluate(samples: list[Sample], show_errors: int = 0) -> dict:
    by_threshold = {t: Confusion() for t in THRESHOLDS}
    by_category: dict[str, Confusion] = {}
    unassessable = 0
    fns, fps = [], []
    for s in samples:
        score, ids = score_sample(s)
        if score is None:
            unassessable += 1
            score = 0  # not flagged: counted conservatively and reported separately
        for t, cm in by_threshold.items():
            cm.add(s.malicious, score >= t)
        by_category.setdefault(s.category, Confusion()).add(s.malicious, score >= FLAG_THRESHOLD)
        if show_errors:
            if s.malicious and score < FLAG_THRESHOLD:
                fns.append((s, score, ids))
            elif not s.malicious and score >= FLAG_THRESHOLD:
                fps.append((s, score, ids))
    report = {
        "ruleset_version": RULESET_VERSION,
        "samples": len(samples),
        "malicious": sum(s.malicious for s in samples),
        "benign": sum(not s.malicious for s in samples),
        "unassessable_inputs": unassessable,
        "operating_points": {
            f"score>={t}": cm.to_dict() for t, cm in by_threshold.items()
        },
        "by_category_at_flag_threshold": {
            c: cm.to_dict() for c, cm in sorted(by_category.items())
        },
    }
    if show_errors:
        fmt = lambda e: {
            "text": defang(e[0].text) if e[0].kind == "url" else e[0].text[:140],
            "category": e[0].category, "score": e[1], "findings": e[2],
        }
        report["missed_examples"] = [fmt(e) for e in fns[:show_errors]]
        report["false_alarm_examples"] = [fmt(e) for e in fps[:show_errors]]
    return report


def _pct(v):
    return "  n/a" if v is None else f"{v * 100:5.1f}%"


def print_summary(title: str, report: dict) -> None:
    print(f"\n=== {title} | ruleset {report['ruleset_version']} | "
          f"{report['malicious']} malicious + {report['benign']} benign "
          f"({report['unassessable_inputs']} unassessable) ===")
    print(f"{'threshold':>11} {'precision':>9} {'recall':>8} {'F1':>7} {'FPR':>7} {'FNR':>7}  "
          f"{'recall 95% CI':>15} {'FPR 95% CI':>15} {'prec@1%prev':>11}")
    for name, m in report["operating_points"].items():
        rlo, rhi = m["recall_ci95"]; flo, fhi = m["false_positive_rate_ci95"]
        print(f"{name:>11} {_pct(m['precision']):>9} {_pct(m['recall']):>8} {_pct(m['f1']):>7} "
              f"{_pct(m['false_positive_rate']):>7} {_pct(m['false_negative_rate']):>7}  "
              f"{rlo*100:5.1f}-{rhi*100:5.1f}%  {flo*100:5.1f}-{fhi*100:5.1f}%  "
              f"{_pct(m['precision_at_1pct_prevalence']):>11}")
    print(f"\nper category at score>={FLAG_THRESHOLD}:")
    for cat, m in report["by_category_at_flag_threshold"].items():
        if m["positives"]:
            print(f"  {cat:28s} malicious n={m['positives']:5d}  recall {_pct(m['recall'])}")
        if m["negatives"]:
            print(f"  {cat:28s} benign    n={m['negatives']:5d}  false alarms {_pct(m['false_positive_rate'])}")


def main(argv=None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--dataset", choices=["seed", "external"], default="seed")
    p.add_argument("--split", choices=["dev", "test", "all"], default="dev")
    p.add_argument("--seed", type=int, default=1337)
    p.add_argument("--show-errors", type=int, default=0)
    p.add_argument("--out")
    p.add_argument("--write-seed-baseline", action="store_true",
                   help="Lock current seed results as the regression baseline (review the diff first).")
    args = p.parse_args(argv)

    if args.dataset == "seed":
        samples = load_jsonl(DATA_DIR / "seed_urls.jsonl") + load_jsonl(DATA_DIR / "seed_messages.jsonl")
        title = "SEED (synthetic regression set, NOT an accuracy estimate)"
        sections = {"urls": [s for s in samples if s.kind == "url"],
                    "messages": [s for s in samples if s.kind == "message"]}
    else:
        if args.split == "test" and args.show_errors:
            print("Refusing to show error examples for the locked TEST split.", file=sys.stderr)
            return 2
        everything = build_external(seed=args.seed)
        samples = [s for s in everything if args.split == "all" or split_of(registered_domain(s.text)) == args.split]
        title = f"EXTERNAL public feeds, {args.split.upper()} split"
        sections = {"urls": samples}

    out = {"generated_at_utc": datetime.now(timezone.utc).isoformat(), "dataset": args.dataset,
           "split": args.split if args.dataset == "external" else "all", "title": title, "sections": {}}
    manifest = EXTERNAL_DIR / "manifest.json"
    if args.dataset == "external" and manifest.exists():
        out["feed_manifest"] = json.loads(manifest.read_text())
    for name, subset in sections.items():
        report = evaluate(subset, show_errors=args.show_errors)
        out["sections"][name] = report
        print_summary(f"{title} / {name}", report)
        for key in ("missed_examples", "false_alarm_examples"):
            for ex in report.get(key, []):
                print(f"   {key[:6]:6s} score={ex['score']:3d} [{ex['category']}] {ex['text']}  {ex['findings']}")
    if args.write_seed_baseline:
        if args.dataset != "seed":
            print("--write-seed-baseline only applies to --dataset seed", file=sys.stderr)
            return 2
        baseline = {"ruleset_version": RULESET_VERSION, "flag_threshold": FLAG_THRESHOLD}
        for name, rep in out["sections"].items():
            m = rep["operating_points"][f"score>={FLAG_THRESHOLD}"]
            baseline[name] = {k: m[k] for k in ("tp", "fp", "tn", "fn")}
        (DATA_DIR.parent / "baselines" / "seed_baseline.json").write_text(json.dumps(baseline, indent=2) + "\n")
        print("seed baseline written")
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            json.dump(out, f, indent=2)
        print(f"\nreport written to {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
