"""Pure automatic approval decision. Authorization is enforced by callers."""
from dataclasses import dataclass


@dataclass(frozen=True)
class ApprovalPolicy:
    enabled: bool = False
    max_age: int = 6
    trusted_sources: frozenset[str] = frozenset()


@dataclass(frozen=True)
class Candidate:
    source_id: str
    age: int
    fingerprint: str
    verified: bool
    license_verified: bool


def may_auto_approve(policy: ApprovalPolicy, candidate: Candidate) -> bool:
    return (
        policy.enabled
        and candidate.verified
        and candidate.license_verified
        and bool(candidate.fingerprint)
        and 0 <= candidate.age <= policy.max_age <= 18
        and candidate.source_id in policy.trusted_sources
    )
