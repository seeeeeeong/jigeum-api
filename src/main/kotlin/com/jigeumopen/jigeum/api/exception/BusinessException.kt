package com.jigeumopen.jigeum.api.exception

import org.springframework.http.HttpStatus

open class BusinessException : RuntimeException {
    val errorCode: ErrorCode
    val status: HttpStatus
    val detail: String?

    constructor(errorCode: ErrorCode) : super(errorCode.message) {
        this.errorCode = errorCode
        this.status = errorCode.status
        this.detail = null
    }

    constructor(errorCode: ErrorCode, detail: String) : super(errorCode.withDetail(detail)) {
        this.errorCode = errorCode
        this.status = errorCode.status
        this.detail = detail
    }

    constructor(errorCode: ErrorCode, detail: String, cause: Throwable) : super(errorCode.withDetail(detail), cause) {
        this.errorCode = errorCode
        this.status = errorCode.status
        this.detail = detail
    }

    companion object {
        // 404 Not Found
        fun notFound(entityName: String, id: Any): BusinessException {
            return BusinessException(
                ErrorCode.CAFE_NOT_FOUND,
                "${entityName}을(를) 찾을 수 없습니다. id: $id"
            )
        }

        // 409 Conflict
        fun duplicate(entityName: String, value: String): BusinessException {
            return BusinessException(
                ErrorCode.DUPLICATE_CAFE,
                "이미 존재하는 ${entityName}입니다: $value"
            )
        }

        // 400 Bad Request
        fun invalidParameter(message: String): BusinessException {
            return BusinessException(ErrorCode.INVALID_PARAMETER, message)
        }

        // 503 Service Unavailable
        fun externalApi(message: String, cause: Throwable? = null): BusinessException {
            return if (cause != null) {
                BusinessException(ErrorCode.EXTERNAL_API_ERROR, message, cause)
            } else {
                BusinessException(ErrorCode.EXTERNAL_API_ERROR, message)
            }
        }

        // 500 Internal Server Error
        fun internal(message: String, cause: Throwable? = null): BusinessException {
            return if (cause != null) {
                BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, message, cause)
            } else {
                BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, message)
            }
        }
    }
}
