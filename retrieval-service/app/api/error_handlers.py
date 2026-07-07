from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.domain.exceptions import RetrievalError


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(RetrievalError)
    async def retrieval_error_handler(
        _request: Request,
        exc: RetrievalError,
    ) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={"detail": str(exc)},
        )
