"""Confusion-matrix metrics with honest uncertainty."""
import math
from dataclasses import dataclass


def wilson(successes: int, total: int, z: float = 1.96) -> tuple[float, float]:
    """95% Wilson score interval for a proportion (well-behaved for small n)."""
    if total == 0:
        return (0.0, 1.0)
    p = successes / total
    denom = 1 + z * z / total
    centre = (p + z * z / (2 * total)) / denom
    half = z * math.sqrt(p * (1 - p) / total + z * z / (4 * total * total)) / denom
    return (max(0.0, centre - half), min(1.0, centre + half))


def _ratio(num: int, den: int) -> float | None:
    return num / den if den else None


@dataclass
class Confusion:
    tp: int = 0
    fp: int = 0
    tn: int = 0
    fn: int = 0

    def add(self, actual_malicious: bool, flagged: bool) -> None:
        if actual_malicious and flagged:
            self.tp += 1
        elif actual_malicious:
            self.fn += 1
        elif flagged:
            self.fp += 1
        else:
            self.tn += 1

    @property
    def positives(self) -> int:
        return self.tp + self.fn

    @property
    def negatives(self) -> int:
        return self.fp + self.tn

    def precision_at_prevalence(self, prevalence: float) -> float | None:
        """
        Precision if only `prevalence` of scanned items were malicious.

        A balanced evaluation sample flatters precision. This re-weights the
        measured recall and false-positive rate to a realistic base rate.
        """
        recall = _ratio(self.tp, self.positives)
        fpr = _ratio(self.fp, self.negatives)
        if recall is None or fpr is None:
            return None
        denom = recall * prevalence + fpr * (1 - prevalence)
        return recall * prevalence / denom if denom else None

    def to_dict(self) -> dict:
        recall = _ratio(self.tp, self.positives)
        fpr = _ratio(self.fp, self.negatives)
        precision = _ratio(self.tp, self.tp + self.fp)
        f1 = (
            2 * precision * recall / (precision + recall)
            if precision and recall
            else (0.0 if precision is not None and recall is not None else None)
        )
        return {
            "tp": self.tp, "fp": self.fp, "tn": self.tn, "fn": self.fn,
            "positives": self.positives, "negatives": self.negatives,
            "precision": precision,
            "recall": recall,
            "f1": f1,
            "false_positive_rate": fpr,
            "false_negative_rate": _ratio(self.fn, self.positives),
            "recall_ci95": wilson(self.tp, self.positives),
            "false_positive_rate_ci95": wilson(self.fp, self.negatives),
            "precision_at_1pct_prevalence": self.precision_at_prevalence(0.01),
        }
