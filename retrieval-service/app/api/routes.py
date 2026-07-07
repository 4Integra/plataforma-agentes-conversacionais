from fastapi import APIRouter, Depends, Header, HTTPException, Request

from app.domain.entities import SearchDocuments
from app.application.retrieval_service import RetrievalService
from app.api.schemas import DocumentIn, DocumentOut, HealthOut, SearchIn
from app.api.schemas import SearchOut, SearchResultOut

router = APIRouter()


def get_retrieval_service(request: Request) -> RetrievalService:
    return request.app.state.retrieval_service


def get_user_id(x_user_id: str | None = Header(default=None)) -> str:
    if x_user_id is None or not x_user_id.strip():
        raise HTTPException(status_code=401, detail="X-User-Id header is required.")

    return x_user_id.strip()


@router.post("/documents", response_model=DocumentOut, status_code=202)
def add_document(
    document: DocumentIn,
    user_id: str = Depends(get_user_id),
    retrieval_service: RetrievalService = Depends(get_retrieval_service),
):
    enqueued_document = retrieval_service.enqueue_document(
        user_id=user_id,
        text=document.text,
        metadata=document.metadata,
    )

    return DocumentOut(
        status="queued",
        id=enqueued_document.id,
        queue_name=enqueued_document.queue_name,
        collection_name=enqueued_document.collection_name,
        model_name=enqueued_document.model_name,
    )


@router.post("/search", response_model=SearchOut)
def search_documents(
    search: SearchIn,
    user_id: str = Depends(get_user_id),
    retrieval_service: RetrievalService = Depends(get_retrieval_service),
):
    results = retrieval_service.search(
        SearchDocuments(
            user_id=user_id,
            query=search.query,
            limit=search.limit,
        )
    )

    return SearchOut(
        results=[
            SearchResultOut(
                score=result.score,
                text=result.text,
                metadata=result.metadata,
                model_name=result.model_name,
            )
            for result in results
        ]
    )


@router.get("/health", response_model=HealthOut)
def health():
    return HealthOut(status="ok")
