package com.jigeumopen.jigeum.api.exception

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val code: String? = null,
    val message: String?,
    val detail: String? = null,
    val path: String? = null,
    val errors: List<FieldError>? = null
) {
    data class FieldError(
        val field: String,
        val value: Any?,
        val message: String?
    )

    companion object {
        fun of(status: HttpStatus, message: String?, path: String? = null): ErrorResponse {
            return ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = path
            )
        }

        fun of(status: HttpStatus, message: String?, path: String?, errors: List<FieldError>): ErrorResponse {
            return ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = path,
                errors = errors
            )
        }

        fun of(e: BusinessException, path: String? = null): ErrorResponse {
            return ErrorResponse(
                status = e.status.value(),
                error = e.status.reasonPhrase,
                code = e.errorCode.code,
                message = e.errorCode.message,
                detail = e.detail,
                path = path
            )
        }
    }
}
