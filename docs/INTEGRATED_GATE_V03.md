> Актуализация 0.3.0: обязательный backend из этого прежнего документа отменён. Главный контракт — [ARCHITECTURE_ANDROID_FIRST_V03.md](ARCHITECTURE_ANDROID_FIRST_V03.md); фактическая реализация и испытания — [ARCHITECTURE.md](ARCHITECTURE.md) и [QA](../QA.md). Android не требует домашнего сервера.

> **Superseded for architecture:** This document originally assumed a mandatory PostgreSQL/backend deployment. The accepted design is Android-first with no mandatory server. See [ARCHITECTURE_ANDROID_FIRST_V03.md](ARCHITECTURE_ANDROID_FIRST_V03.md). Treat PostgreSQL and backend authorization tasks below as optional future integrations, not release gates.

# Integrated implementation gate — Family Cinema 0.3

This is a single integrated engineering gate, not permission to claim all features shipped.

## Prepared directly in GitHub
- 0.2.0 Compose mobile/tablet/TV and Media3 implementation retained.
- Opt-in automatic approval switch in parent-only UI, confirmation, private preference, ViewModel guard; default OFF. **No automatic discovery approval yet.**
- Backend title normalization, local SQLite catalog repository, verified exact-version approvals, revoke-on-fingerprint-change, profile scoping, automatic approval pure policy; Python tests.
- Search and catalog architecture: docs/CATALOG_SEARCH_V03.md.

## Mandatory engineering tasks before production API
1. Review all added files for Python import/format/type/SQL correctness; run pytest in the backend environment. Fix failures and security gaps.
2. PostgreSQL migration with transactional schema, indexes, foreign keys, unique constraints; keep SQLite only as development/test adapter. Design explicit migration strategy and backup/rollback.
3. Authenticated parent/child API with short-lived tokens, secure device pairing and revocation. No fake auth via headers or child-supplied profile IDs. Enforce per-profile permissions at the service layer and API boundary.
4. Content metadata provider integration only after verifying terms, image attribution, API key handling and caching. Russian titles and descriptions preferred, alternatives searchable. No fabricated posters/results.
5. Android API repository, offline cache, real search and catalogue screens; responsive Compose on phone/tablet/TV, loading/error/empty states. Preserve existing working player and approval migration.
6. Auto-approval end-to-end: parent-only setting, explicit consent, trusted source allowlist, per-profile age cap, content license and version validation, fingerprint binding, audit log, revocation. OFF on new/upgrade; no broad automatic approval of unverified search hits.
7. Integration tests: unauthorized read/write, child cannot list unapproved works, token expiry, revoked media, fingerprint changes, duplicate imports, pagination, provider errors, SQL migration, UI D-pad, background/resume, PlayBack regression.
8. Cloud build + screenshots and real APK delivered via authorized Google Drive connector with roundtrip checksum. No GitHub Actions requirement.

## Boundaries
- No external torrent downloads until source compliance, permissions, sandboxing, streaming budget and abuse limits are designed and tested.
- Do not call a local SQLite proof-of-concept production PostgreSQL.
- Do not declare automatic approval operational while no trusted sources and age policies are configured.
- Do not merge PR #2 blindly: it is stacked on PR #1; preserve all previous changes and handle branch relationships.
- Debug APK and emulator-only tests do not prove physical TV compatibility.
