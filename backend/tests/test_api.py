import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError
from app.main import app
from app.contracts import SourceConfig, SearchQuery, VideoVersion

client = TestClient(app)

def test_health_is_honest_about_database():
    assert client.get("/health").json() == {"status": "ok", "stage": 1, "database": "not_configured"}

def test_empty_catalog_does_not_invent_results():
    assert client.get("/catalog").json() == []

@pytest.mark.parametrize("endpoint", ["http://example.org/api", "https://user:secret@example.org/api"])
def test_reject_unsafe_source(endpoint):
    with pytest.raises(ValidationError):
        SourceConfig(id="a", kind="torznab", endpoint=endpoint)

def test_source_uses_credential_reference():
    assert SourceConfig(id="a",kind="torznab",endpoint="https://example.org/api",credential_alias="indexer").credential_alias == "indexer"

@pytest.mark.parametrize("payload", [{"title_ru": ""}, {"title_ru": "ёжик", "episode": 0}, {"title_ru": "ёжик", "year": 100}])
def test_invalid_queries(payload):
    with pytest.raises(ValidationError):
        SearchQuery(**payload)

def test_digest_required():
    with pytest.raises(ValidationError):
        VideoVersion(id="one", sha256="filename-is-not-proof")
