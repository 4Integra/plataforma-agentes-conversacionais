class RetrievalError(Exception):
    status_code = 400


class InvalidUserError(RetrievalError):
    status_code = 401


class InvalidDocumentError(RetrievalError):
    status_code = 422


class DocumentTooLargeError(RetrievalError):
    status_code = 413


class QueryTooLargeError(RetrievalError):
    status_code = 413


class MetadataTooLargeError(RetrievalError):
    status_code = 413


class InvalidLimitError(RetrievalError):
    status_code = 422


class CollectionConfigurationError(RetrievalError):
    status_code = 409


class IngestionQueueUnavailableError(RetrievalError):
    status_code = 503
