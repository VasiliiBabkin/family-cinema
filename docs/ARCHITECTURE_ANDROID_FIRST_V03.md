# Family Cinema 0.3 — Android-first, no mandatory server

Decision: Android phone/tablet/TV is the complete primary runtime. A household PC, PostgreSQL, Jellyfin, Prowlarr, cloud account and hosted backend are **optional**. The earlier backend prototype remains an isolated experiment, not an app dependency.

## Verified open-source reference choices (2026-10-26)

| Project | Role | Integration decision |
| --- | --- | --- |
| AndroidX Media3 | Native playback | Keep current ExoPlayer and Compose fullscreen implementation |
| Stremio Addon Protocol | Open JSON format for catalog/meta/stream/subtitles | Implement Kotlin protocol client; do **not** embed the Node.js SDK |
| Jellyfin Kotlin SDK (LGPL-3.0) | Optional household server connector | Future opt-in connector; never a required login/server |
| TMDB API | Optional metadata provider | Only after API key and commercial-use/attribution review |
| libtorrent / Android wrappers | Future on-device torrent engine | Separate optional feature; licensing, ARM64, seek/cache, security and Android lifecycle must be tested before integration |
| Prowlarr, Seerr, Radarr/Sonarr | Server-side services | Not bundled and not required; optional advanced integrations only |

Source links:
- https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/protocol.md
- https://github.com/jellyfin/jellyfin-sdk-kotlin
- https://developer.themoviedb.org/docs/faq

## Device-owned architecture

```text
Family Cinema Android (phone / tablet / TV)
 ├── Compose UI: child, parent, search, details, episodes, settings
 ├── Local repository: Room/SQLite + migrations, cached metadata and images
 ├── Local parental gate: PIN, verified parent session, profiles, age/source policies
 ├── Catalog aggregator: local files + configured remote addons + optional metadata
 ├── Sources: Stremio-protocol adapter, trusted HTTP media, optional Jellyfin
 ├── Candidate verification: stable work/episode/source/version identity
 ├── Playback: Media3 + future on-device torrent module (not required for HTTP)
 └── Progress and approvals: local-first, optionally export/sync
```

No remote service is required for basic local playback, PIN, manual approvals, progress and browsing cached metadata. Online catalog search naturally needs connectivity to the selected remote provider, but not a server owned by the user.

## Implemented foundation

- Existing 0.2.0 native Compose screens, Media3 player, private PIN and approval state.
- Parent-only switch “Одобрять автоматически” with explicit confirmation, OFF by default.
- Typed read-only Kotlin Stremio-protocol `AddonProtocol` and `AddonEndpoint`, JSON parsing, source tests.
- `HttpsAddonTransport` with explicit trusted hosts, HTTPS, no redirects, bounded response and timeouts. **Not safe for arbitrary/untrusted hostnames:** DNS validation and connection resolution are not atomic; production needs a pinned-resolution network stack or an explicit operator-controlled allowlist.
- Backend SQLite proof-of-concept and search tests remain for reference. Do not force backend into Android.

## Local-first data model

- Work(id, title_ru, original_title, aliases, type, year, description, poster attribution, age).
- Episode(id, work_id, season, number).
- Candidate(source_id, work_id, episode_id, locator, claimed media attributes).
- VerifiedVersion(work_id, episode_id, source_id, locator, verified content identity).
- Approval(profile_id, version_fingerprint, manual|automatic, created_at, revoked_at).
- Profile(id, age_limit, trusted_sources, auto_approval).
- Progress(profile_id, verified_version, position_ms, updated_at).
- SourceSettings(id, endpoint, enabled, credentials stored securely, trust state).

`Candidate` never grants playback by itself. Hashing a magnet link or torrent infohash proves only that identifier, not that the video is appropriate for a child. Do not mark it as verified content without checking actual media and provenance.

## Auto-approval rules

- Parent-only, explicit confirmation; disabled by default including upgrades.
- Default policy has no trusted remote sources; no new content is automatically allowed merely because toggle is on.
- Require verified version identity, age suitability, explicit trusted source and content-rights eligibility. Fail closed on missing data.
- Manual revoke should prevent re-approval of the **same fingerprint** until a new parent decision; implement persistent deny/tombstone before enabling automatic discovery.
- Turning auto-approval off prevents future grants, not silent mass revocation.
- Do not expose unapproved candidates, source credentials, magnet links or raw playback locators to child UI.
- Multiple profiles need per-profile age/trust policy; never inherit parent permission.

## Planned single integrated delivery

1. Fix branch/PR stacking and run JVM/backend tests, Android compile and lint.
2. Build Android Room entities/DAO/repository and migration from current SharedPreferences; preserve PIN, demo approval and progress. Do not introduce a required PostgreSQL server.
3. Add source management behind PIN; trust explicit hosts; real Stremio protocol manifest validation and catalog/meta/stream parsing; network permission only when functional, privacy-safe networking.
4. Add optional metadata provider and image caching with attribution and licensing. Local search and offline cache work without network.
5. Implement policy engine and revoke tombstones, profile limits, audit trail and atomic approvals.
6. Integrate parent discovery and child approved catalog with Compose UI on phone/tablet/TV.
7. Support playable authorized HTTP media with Media3; separate torrent engine evaluation and ARM64 validation. Do not ship a fake streaming button.
8. Run migration/regression/instrumentation tests and deliver verified APK through private Drive with roundtrip checksum.

## Explicit non-goals for initial Android-first release

- Mandatory PostgreSQL/FastAPI deployment.
- Mandatory Jellyfin/Prowlarr/Seerr/Radarr server.
- Cloud account and remote pairing before local playback.
- Implicit legal rights inferred from metadata or a source's popularity.
- Automatically trusting public Stremio addons.
