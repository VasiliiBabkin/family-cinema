from typing import Literal, Protocol
from pydantic import BaseModel, Field, HttpUrl, model_validator

class VideoVersion(BaseModel):
    id: str = Field(min_length=1)
    sha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    episode: int | None = Field(default=None, ge=1)
    # Видео не передаётся в публичной модели каталога: разрешение версии отдельно.

class CatalogItem(BaseModel):
    id: str = Field(min_length=1)
    title_ru: str = Field(min_length=1)
    age: int = Field(ge=0, le=18)
    license: str = Field(min_length=1)
    version: VideoVersion

class SourceConfig(BaseModel):
    id: str
    kind: Literal["torznab", "documented_api"]
    endpoint: HttpUrl
    credential_alias: str | None = None

    @model_validator(mode="after")
    def secure_endpoint(self):
        if self.endpoint.scheme != "https" or self.endpoint.username or self.endpoint.password:
            raise ValueError("Источник требует HTTPS без учётных данных в URL")
        return self

class SearchQuery(BaseModel):
    title_ru: str = Field(min_length=1, max_length=200)
    year: int | None = Field(default=None, ge=1888, le=2100)
    season: int | None = Field(default=None, ge=1)
    episode: int | None = Field(default=None, ge=1)

class SourceAdapter(Protocol):
    async def search(self, query: SearchQuery) -> list[CatalogItem]: ...
