# Evaluation datasets: provenance, licence and limits

Reproduce everything from `backend/`:

```
python -m evaluation.fetch_external                              # downloads the two list files (never any phishing URL)
python -m evaluation.run_eval --dataset seed                     # synthetic regression set
python -m evaluation.run_eval --dataset external --split dev --show-errors 25   # tune against DEV only
python -m evaluation.run_eval --dataset external --split test    # locked: aggregates only, run once per release
python -m pytest tests/test_evaluation_regression.py             # regression guard + harness tests
```

Nothing here opens, resolves or fetches any evaluated URL. The analyser only reads strings. Reports defang
URLs (`hxxp://x[.]com`). Third-party URLs are never committed: only the code, the manifest hashes and aggregate numbers are.

## 1. `seed_urls.jsonl` / `seed_messages.jsonl` (committed)

| | |
|---|---|
| Provenance | **Synthetic.** Hand-written by the project team: realistic benign links and messages, scam patterns (including Kenyan M-PESA, loan-fee and prize scams, and some Swahili), deliberate evasion attempts, and "hard" cases expected to be missed. |
| Licence | Project-owned. |
| Purpose | **Regression guard only.** Fails the build if the detector starts missing known cases or raising new false alarms. |
| Limits | The detector was developed while looking at patterns like these, so results here are **optimistic and are NOT an accuracy estimate.** Domains in malicious samples are invented and may coincide with real ones; nothing is ever fetched. Messages were written by us, not collected from victims. |

## 2. External URL set (downloaded at run time, git-ignored)

| | Malicious | Benign |
|---|---|---|
| Source | Phishing.Database `phishing-links-ACTIVE.txt` (github.com/Phishing-Database/Phishing.Database) | OpenDNS `opendns-top-domains.txt` (github.com/opendns/public-domain-lists) |
| Licence | MIT (Copyright Mitchell Krog and Phishing.Database contributors), confirmed on the project README | **UNVERIFIED**: no LICENSE file found. Used locally for evaluation only, never redistributed. |
| Retrieved | 2026-10-18 (exact time, size and SHA-256 recorded in `manifest.json` and in every report) | same |
| Sampling | Deterministic (`--seed 1337`): up to 2,000 URLs, **max 2 per registered domain** so one campaign cannot dominate | 2,000 domains, shown as `https://<domain>/` |
| Split | 50/50 dev/test, **keyed by registered domain**, so no campaign or site appears on both sides (no leakage) | same |

### Known limits (read before quoting any number)
1. **Label noise.** "ACTIVE" means the URL still answered. The list aggregates third-party feeds and includes compromised legitimate
   sites. Some entries may be benign or already cleaned. True recall is unknowable from this data.
2. **Benign set has no deep links.** It contains domain root pages only, so *false-alarm rates for path- and query-based rules are NOT measured.*
   Any new rule that reads paths or query strings needs a benign deep-link corpus before it can be trusted. All benign URLs are `https`, which
   makes the weak "no HTTPS" signal look more discriminating than it is. **Thresholds below 25 are therefore flattered; use score>=25 or higher.**
3. **Popularity bias.** Top-10k domains are the easiest benign case. Small, new or hyphenated legitimate sites would score worse.
4. **No time dimension.** One snapshot: no time-based evaluation of emerging threats yet.
5. **Not a test of threat-intelligence lookups.** Checking URLs against this same feed would score ~100% trivially. Lookup quality must be
   measured against an independent source or a later time window.
6. **Per-category labels are derived from the URL string** (IP host / shared platform / own domain), independently of the detector.
7. **No SMS or email corpus.** No licensed, representative scam-message dataset has been sourced. Message results (seed only) say nothing
   about real-world accuracy. Collecting consented, anonymised real examples is the next step for that.
8. The benign set contains domains that look like their brand's but are not provably so (e.g. `member-hsbc-group.com`) and
   `paypal-communication.com`, which forum and blog evidence suggests PayPal really uses. These are recorded false alarms, left unresolved until a
   primary source confirms ownership. A wrong "official" entry would make the scanner trust an impostor.

## Discipline
Tune only against `--split dev`. `--split test` refuses `--show-errors`. Run it once per released ruleset and record the report in
`reports/`. Changing the ruleset changes `RULESET_VERSION`; the regression test then demands a reviewed baseline refresh.
