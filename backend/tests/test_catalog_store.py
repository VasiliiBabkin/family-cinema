import sqlite3
import pytest
from app.catalog_store import CatalogStore, Work
from app.approval_policy import ApprovalPolicy, Candidate, may_auto_approve


@pytest.fixture
def store():
    db = sqlite3.connect(":memory:")
    s = CatalogStore(db)
    s.initialize()
    s.put_work(Work("hedgehog", "Ёжик в тумане", 1975, "animation", 0, ("Hedgehog in the Fog",)))
    return s


def test_search_russian_latin_and_alias(store):
    assert [x.id for x in store.find_works("ежик")] == ["hedgehog"]
    assert [x.id for x in store.find_works("ezhik")] == ["hedgehog"]
    assert [x.id for x in store.find_works("hedgehog")] == ["hedgehog"]


def test_empty_and_bounded_pagination(store):
    assert store.find_works("") == []
    assert store.find_works("ежик", offset=1) == []
    with pytest.raises(ValueError):
        store.find_works("ежик", limit=101)


def test_unverified_version_cannot_be_approved(store):
    store.put_version(version_id="v1", work_id="hedgehog", source_id="trusted",
                      sha256="a"*64, locator="local://video", fingerprint="v1-a")
    assert not store.approve("child", "v1")
    assert store.child_works("child") == []


def test_verified_approval_and_revoke(store):
    store.put_version(version_id="v1", work_id="hedgehog", source_id="trusted",
                      sha256="a"*64, locator="local://video", fingerprint="v1-a", verified=True)
    assert store.approve("child", "v1")
    assert store.approved("child", "v1")
    assert store.child_works("child") == ["hedgehog"]
    assert not store.approved("other", "v1")
    store.revoke("child", "v1")
    assert store.child_works("child") == []


def test_changed_fingerprint_revokes_old_approval(store):
    store.put_version(version_id="v1", work_id="hedgehog", source_id="trusted",
                      sha256="a"*64, locator="local://video", fingerprint="v1-a", verified=True)
    assert store.approve("child", "v1")
    store.put_version(version_id="v1", work_id="hedgehog", source_id="trusted",
                      sha256="b"*64, locator="local://new", fingerprint="v1-b", verified=True)
    assert not store.approved("child", "v1")


def test_auto_approval_default_off_and_requirements():
    c = Candidate("trusted", 6, "sha256:v1", True, True)
    assert not may_auto_approve(ApprovalPolicy(), c)
    p = ApprovalPolicy(True, 6, frozenset({"trusted"}))
    assert may_auto_approve(p, c)
    assert not may_auto_approve(p, Candidate("untrusted", 6, "x", True, True))
    assert not may_auto_approve(p, Candidate("trusted", 12, "x", True, True))
    assert not may_auto_approve(p, Candidate("trusted", 6, "x", False, True))
    assert not may_auto_approve(p, Candidate("trusted", 6, "x", True, False))
