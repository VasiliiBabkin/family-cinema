"""Internal catalog persistence; no public endpoints or authorization shortcuts.

SQLite supports local development and unit tests. Production PostgreSQL integration
and migrations must be completed before exposing authenticated endpoints.
"""
from __future__ import annotations

import json
import sqlite3
from dataclasses import dataclass
from hashlib import sha256


@dataclass(frozen=True)
class Work:
    id: str
    title_ru: str
    year: int | None
    kind: str
    age: int
    aliases: tuple[str, ...] = ()
    description: str = ""
    poster_url: str | None = None
    poster_license: str | None = None


class CatalogStore:
    def __init__(self, db: sqlite3.Connection):
        self.db = db
        self.db.execute("PRAGMA foreign_keys=ON")

    def initialize(self) -> None:
        self.db.executescript("""
            CREATE TABLE IF NOT EXISTS works (
                id TEXT PRIMARY KEY,
                title_ru TEXT NOT NULL CHECK(length(trim(title_ru)) > 0),
                year INTEGER CHECK(year IS NULL OR year BETWEEN 1888 AND 2100),
                kind TEXT NOT NULL CHECK(kind IN ('movie','series','animation')),
                age INTEGER NOT NULL CHECK(age BETWEEN 0 AND 18),
                aliases_json TEXT NOT NULL DEFAULT '[]',
                description TEXT NOT NULL DEFAULT '',
                poster_url TEXT,
                poster_license TEXT
            );
            CREATE TABLE IF NOT EXISTS episodes (
                id TEXT PRIMARY KEY,
                work_id TEXT NOT NULL REFERENCES works(id) ON DELETE CASCADE,
                season INTEGER NOT NULL CHECK(season >= 1),
                episode INTEGER NOT NULL CHECK(episode >= 1),
                title_ru TEXT NOT NULL DEFAULT '',
                UNIQUE(work_id, season, episode)
            );
            CREATE TABLE IF NOT EXISTS media_versions (
                id TEXT PRIMARY KEY,
                work_id TEXT NOT NULL REFERENCES works(id) ON DELETE CASCADE,
                episode_id TEXT REFERENCES episodes(id) ON DELETE CASCADE,
                source_id TEXT NOT NULL,
                sha256 TEXT NOT NULL CHECK(length(sha256) = 64),
                locator TEXT NOT NULL,
                fingerprint TEXT NOT NULL UNIQUE,
                verified INTEGER NOT NULL DEFAULT 0 CHECK(verified IN (0,1)),
                CHECK(length(locator) > 0)
            );
            CREATE TABLE IF NOT EXISTS approvals (
                profile_id TEXT NOT NULL,
                media_version_id TEXT NOT NULL REFERENCES media_versions(id) ON DELETE CASCADE,
                fingerprint TEXT NOT NULL,
                approved_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY(profile_id, media_version_id)
            );
            CREATE INDEX IF NOT EXISTS idx_works_title ON works(title_ru);
            CREATE INDEX IF NOT EXISTS idx_media_work ON media_versions(work_id);
        """)
        self.db.commit()

    def put_work(self, work: Work) -> None:
        if not work.id or not work.title_ru.strip() or work.kind not in {"movie", "series", "animation"}:
            raise ValueError("Invalid work")
        if not 0 <= work.age <= 18:
            raise ValueError("Invalid age")
        self.db.execute(
            """INSERT INTO works(id,title_ru,year,kind,age,aliases_json,description,poster_url,poster_license)
            VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
            title_ru=excluded.title_ru,year=excluded.year,kind=excluded.kind,
            age=excluded.age,aliases_json=excluded.aliases_json,
            description=excluded.description,poster_url=excluded.poster_url,
            poster_license=excluded.poster_license""",
            (work.id, work.title_ru, work.year, work.kind, work.age,
             json.dumps(work.aliases, ensure_ascii=False), work.description,
             work.poster_url, work.poster_license),
        )
        self.db.commit()

    def find_works(self, query: str, *, limit: int = 20, offset: int = 0) -> list[Work]:
        from .search_normalization import title_matches
        if not 1 <= limit <= 100 or offset < 0:
            raise ValueError("Invalid pagination")
        if not query.strip():
            return []
        # Explicit bounded local implementation. Use indexed search for production.
        rows = self.db.execute(
            "SELECT id,title_ru,year,kind,age,aliases_json,description,poster_url,poster_license FROM works ORDER BY title_ru,id"
        )
        matched = []
        for row in rows:
            aliases = tuple(json.loads(row[5]))
            if title_matches(query, row[1], aliases):
                matched.append(Work(row[0], row[1], row[2], row[3], row[4], aliases,
                                    row[6], row[7], row[8]))
                if len(matched) >= offset + limit:
                    break
        return matched[offset:offset + limit]

    @staticmethod
    def version_fingerprint(work_id: str, episode_id: str | None, source_id: str,
                            digest: str, locator: str) -> str:
        identity = json.dumps(
            [work_id, episode_id, source_id, digest, locator],
            ensure_ascii=False, separators=(",", ":"),
        )
        return sha256(identity.encode("utf-8")).hexdigest()

    def put_version(self, *, version_id: str, work_id: str, source_id: str,
                    sha256: str, locator: str, fingerprint: str,
                    episode_id: str | None = None, verified: bool = False) -> None:
        if len(sha256) != 64 or any(c not in "0123456789abcdef" for c in sha256):
            raise ValueError("Invalid digest")
        if not all((version_id, work_id, source_id, locator, fingerprint)):
            raise ValueError("Missing version identity")
        # Reject arbitrary client-chosen identity strings: bind approvals to the
        # exact source, content digest, location, work and episode.
        expected = self.version_fingerprint(work_id, episode_id, source_id, sha256, locator)
        if fingerprint != expected:
            raise ValueError("Version fingerprint does not match media identity")
        self.db.execute(
            """INSERT INTO media_versions(id,work_id,episode_id,source_id,sha256,locator,fingerprint,verified)
            VALUES (?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET
            work_id=excluded.work_id,episode_id=excluded.episode_id,
            source_id=excluded.source_id,sha256=excluded.sha256,
            locator=excluded.locator,fingerprint=excluded.fingerprint,
            verified=excluded.verified""",
            (version_id, work_id, episode_id, source_id, sha256, locator, fingerprint, int(verified)),
        )
        # Revocation on identity change is essential: approval never follows a changed file.
        self.db.execute(
            """DELETE FROM approvals WHERE media_version_id=?
               AND fingerprint <> ?""", (version_id, fingerprint)
        )
        self.db.commit()

    def approve(self, profile_id: str, version_id: str) -> bool:
        if not profile_id:
            raise ValueError("Missing profile")
        row = self.db.execute(
            "SELECT fingerprint,verified FROM media_versions WHERE id=?", (version_id,)
        ).fetchone()
        if row is None or not row[1]:
            return False
        self.db.execute(
            """INSERT INTO approvals(profile_id,media_version_id,fingerprint) VALUES (?,?,?)
               ON CONFLICT(profile_id,media_version_id) DO UPDATE SET fingerprint=excluded.fingerprint,
               approved_at=CURRENT_TIMESTAMP""", (profile_id, version_id, row[0])
        )
        self.db.commit()
        return True

    def revoke(self, profile_id: str, version_id: str) -> None:
        self.db.execute(
            "DELETE FROM approvals WHERE profile_id=? AND media_version_id=?",
            (profile_id, version_id),
        )
        self.db.commit()

    def approved(self, profile_id: str, version_id: str) -> bool:
        row = self.db.execute(
            """SELECT 1 FROM approvals a JOIN media_versions v ON v.id=a.media_version_id
               WHERE a.profile_id=? AND v.id=? AND a.fingerprint=v.fingerprint AND v.verified=1""",
            (profile_id, version_id),
        ).fetchone()
        return row is not None

    def child_works(self, profile_id: str) -> list[str]:
        return [r[0] for r in self.db.execute(
            """SELECT DISTINCT v.work_id FROM media_versions v
               JOIN approvals a ON a.media_version_id=v.id
               WHERE a.profile_id=? AND a.fingerprint=v.fingerprint AND v.verified=1
               ORDER BY v.work_id""", (profile_id,)
        )]
