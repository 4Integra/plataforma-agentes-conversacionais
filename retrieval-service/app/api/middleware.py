from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.core.config import Settings


def register_middlewares(app: FastAPI, settings: Settings) -> None:
    @app.middleware("http")
    async def limit_request_body_size(request: Request, call_next):
        content_length = request.headers.get("content-length")

        try:
            request_size = int(content_length) if content_length else 0
        except ValueError:
            return JSONResponse(
                status_code=400,
                content={"detail": "Invalid Content-Length header."},
            )

        if request_size > settings.max_request_bytes:
            return JSONResponse(
                status_code=413,
                content={
                    "detail": (
                        "Request body must have at most "
                        f"{settings.max_request_bytes} bytes."
                    )
                },
            )

        return await call_next(request)
