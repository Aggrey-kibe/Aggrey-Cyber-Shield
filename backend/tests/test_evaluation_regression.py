"""
Regression guard + unit tests for the evaluation harness.

The seed sets are SYNTHETIC. They prove the detector has not got worse on known
cases; they are not an estimate of real-world accuracy (see evaluation/DATASETS.md).
To accept a deliberate change, review it, then run:
    python -m evaluation.run_eval --dataset seed --write-seed-baseline
"""
import json
from pathlib import Path

import pytest

from app.services.scanning import RULESET_VERSION
from evaluation import fetch_external
from evaluation.datasets import (
    DATA_DIR, build_external, defang, load_jsonl, registered_domain, split_of, url_category,
)
from evaluation.metrics import Confusion, wilson
from evaluation.run_eval import FLAG_THRESHOLD, evaluate

BASELINE = json.loads((Path(__file__).parent.parent / "evaluation/baselines/seed_baseline.json").read_text())


def _seed(kind_file):
    return load_jsonl(DATA_DIR / kind_file)


@pytest.mark.parametrize("name,file", [("urls", "seed_urls.jsonl"), ("messages", "seed_messages.jsonl")])
def test_detector_has_not_regressed_on_seed_set(name, file):
    m = evaluate(_seed(file))["operating_points"][f"score>={FLAG_THRESHOLD}"]
    base = BASELINE[name]
    assert m["fn"] <= base["fn"], f"{name}: missed MORE malicious samples ({m['fn']} > {base['fn']})"
    assert m["fp"] <= base["fp"], f"{name}: more false alarms ({m['fp']} > {base['fp']})"


def test_seed_sets_are_labelled_and_documented():
    for file in ("seed_urls.jsonl", "seed_messages.jsonl"):
        samples = _seed(file)
        assert len({s.id for s in samples}) == len(samples)          # unique ids
        assert {s.label for s in samples} == {"malicious", "benign"}
        assert all(s.source.startswith("synthetic") and s.license for s in samples)
        assert sum(s.malicious for s in samples) >= 25 and sum(not s.malicious for s in samples) >= 25


def test_baseline_names_the_ruleset_version():
    assert BASELINE["ruleset_version"] == RULESET_VERSION, (
        "Rules changed: re-run the seed evaluation and lock a new baseline after review."
    )


# --------------------------------------------------------------- metrics
def test_confusion_metrics_by_hand():
    cm = Confusion()
    for actual, flagged in [(True, True)] * 8 + [(True, False)] * 2 + [(False, True)] * 1 + [(False, False)] * 9:
        cm.add(actual, flagged)
    d = cm.to_dict()
    assert (d["tp"], d["fn"], d["fp"], d["tn"]) == (8, 2, 1, 9)
    assert d["precision"] == pytest.approx(8 / 9)
    assert d["recall"] == pytest.approx(0.8)
    assert d["false_positive_rate"] == pytest.approx(0.1)
    assert d["false_negative_rate"] == pytest.approx(0.2)
    assert d["f1"] == pytest.approx(2 * (8 / 9) * 0.8 / (8 / 9 + 0.8))


def test_precision_is_reweighted_for_realistic_prevalence():
    cm = Confusion(tp=90, fn=10, fp=10, tn=90)  # recall .9, fpr .1, balanced precision .9
    assert cm.to_dict()["precision"] == pytest.approx(0.9)
    # only 1% of scanned items malicious: precision collapses to ~8%
    assert cm.precision_at_prevalence(0.01) == pytest.approx(0.009 / (0.009 + 0.099))


def test_wilson_interval_behaves():
    lo, hi = wilson(0, 100)
    assert lo == 0.0 and 0.0 < hi < 0.05
    lo, hi = wilson(50, 100)
    assert lo < 0.5 < hi
    assert wilson(0, 0) == (0.0, 1.0)


def test_metrics_with_no_positives_do_not_divide_by_zero():
    d = Confusion(fp=1, tn=9).to_dict()
    assert d["recall"] is None and d["precision"] == 0.0


# --------------------------------------------------------------- datasets
def test_split_is_deterministic_and_keyed_by_domain_so_campaigns_do_not_leak():
    urls = [f"http://campaign{i % 7}.example-{i % 7}.com/page{i}" for i in range(200)]
    first = {u: split_of(registered_domain(u)) for u in urls}
    assert first == {u: split_of(registered_domain(u)) for u in urls}
    by_domain = {}
    for u, side in first.items():
        by_domain.setdefault(registered_domain(u), set()).add(side)
    assert all(len(sides) == 1 for sides in by_domain.values())  # a domain never straddles both splits
    assert {"dev", "test"} <= {split_of(f"d{i}.com") for i in range(50)}


def test_defang_makes_urls_unclickable():
    import re
    out = defang("http://evil.example.com/login")
    assert out.startswith("hxxp://") and "[.]" in out
    assert not re.search(r"(?<!\[)\.(?!\])", out)      # no bare, clickable dots remain
    assert "http" not in defang("https://x.com").replace("hxxp", "")


def test_categories_do_not_depend_on_detector_output():
    assert url_category("http://192.0.2.1/x") == "ip_host"
    assert url_category("https://foo.weeblysite.com/") == "own_domain"
    assert url_category("https://foo.weebly.com/") == "shared_platform"
    assert url_category("https://example.co.ke/") == "own_domain"


def test_downloader_refuses_unlisted_hosts_and_non_https(tmp_path):
    for bad in ("https://evil.example.com/list.txt", "http://raw.githubusercontent.com/x", "file:///etc/passwd"):
        with pytest.raises(ValueError):
            fetch_external._download(bad, tmp_path / "x")


def test_external_sampling_is_reproducible_when_feeds_are_present():
    try:
        first = build_external(seed=7, n_pos=50, n_neg=50)
    except FileNotFoundError:
        pytest.skip("external feeds not downloaded (python -m evaluation.fetch_external)")
    assert [s.id for s in first] == [s.id for s in build_external(seed=7, n_pos=50, n_neg=50)]
    positives = [s for s in first if s.malicious]
    per_domain = {}
    for s in positives:
        per_domain[registered_domain(s.text)] = per_domain.get(registered_domain(s.text), 0) + 1
    assert max(per_domain.values()) <= 2


def test_evaluate_counts_unassessable_inputs_separately():
    from evaluation.datasets import Sample
    bad = Sample("x1", "url", "http://[::1", "malicious", "t", "synthetic", "mit")
    report = evaluate([bad])
    assert report["unassessable_inputs"] == 1
    assert report["operating_points"]["score>=25"]["fn"] == 1
