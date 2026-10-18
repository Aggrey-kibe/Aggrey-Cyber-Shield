"""
Threat-scanning engine for Aggrey Shield.

Everything in this package is a PURE analysis of the text it is given: it never
opens a link, resolves a hostname or contacts any server. That is deliberate:
scanning a hostile URL must never let that URL make this server (or the
user's network) connect anywhere (no SSRF, no tracking pixels, no malware
downloads). Live reputation lookups can be added later as a separate,
carefully-restricted step.
"""

RULESET_VERSION = "heuristic-2026.10.18-1"
