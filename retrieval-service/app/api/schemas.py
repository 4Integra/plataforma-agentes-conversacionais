from typing import Any

from pydantic import BaseModel, Field


class DocumentIn(BaseModel):
    text: str = Field(..., min_length=1)
    metadata: dict[str, Any] = Field(default_factory=dict)

    class Config:
        extra = "forbid"


class SearchIn(BaseModel):
    query: str = Field(..., min_length=1)
    limit: int = Field(default=5, ge=1)

    class Config:
        extra = "forbid"


class DocumentOut(BaseModel):
    status: str
    id: str
    queue_name: str
    collection_name: str
    model_name: str


class SearchResultOut(BaseModel):
    score: float
    text: str | None
    metadata: dict[str, Any]
    model_name: str | None


class SearchOut(BaseModel):
    results: list[SearchResultOut]


class HealthOut(BaseModel):
    status: str
