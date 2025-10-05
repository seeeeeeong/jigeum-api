package com.jigeumopen.jigeum.common.exception

import com.jigeumopen.jigeum.cafe.dto.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handle(e: BusinessException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.error("[${e.errorCode.code}] ${e.message}", e)
        return ResponseEntity.status(e.status)
            .body(ErrorResponse.of(e, request.requestURI))
    }

    @ExceptionHandler(GooglePlacesException::class)
    fun handle(
        e: GooglePlacesException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Google Places API error: ${e.message}", e)

        val (status, code) = when (e) {
            is GooglePlacesException.BadRequest -> HttpStatus.BAD_REQUEST to ErrorCode.INVALID_PARAMETER
            is GooglePlacesException.Unauthorized -> HttpStatus.UNAUTHORIZED to ErrorCode.INVALID_API_KEY
            is GooglePlacesException.NotFound -> HttpStatus.NOT_FOUND to ErrorCode.RESOURCE_NOT_FOUND
            is GooglePlacesException.RateLimited -> HttpStatus.TOO_MANY_REQUESTS to ErrorCode.GOOGLE_API_RATE_LIMIT
            is GooglePlacesException.ServerError -> HttpStatus.BAD_GATEWAY to ErrorCode.GOOGLE_API_ERROR
            is GooglePlacesException.UnknownError -> HttpStatus.INTERNAL_SERVER_ERROR to ErrorCode.EXTERNAL_API_ERROR
        }

        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                code = code.code,
                message = code.message,
                detail = e.message,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map {
            ErrorResponse.FieldError(it.field, it.rejectedValue, it.defaultMessage)
        }

        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                code = ErrorCode.INVALID_PARAMETER.code,
                message = ErrorCode.INVALID_PARAMETER.message,
                path = request.requestURI,
                errors = errors
            )
        )
    }

    @ExceptionHandler(BindException::class)
    fun handle(e: BindException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map {
            ErrorResponse.FieldError(it.field, it.rejectedValue, it.defaultMessage)
        }

        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                code = ErrorCode.INVALID_PARAMETER.code,
                message = ErrorCode.INVALID_PARAMETER.message,
                path = request.requestURI,
                errors = errors
            )
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handle(e: MethodArgumentTypeMismatchException, request: HttpServletRequest) =
        badRequest(ErrorCode.INVALID_PARAMETER, request.requestURI)

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handle(e: MissingServletRequestParameterException, request: HttpServletRequest) =
        badRequest(ErrorCode.INVALID_PARAMETER, request.requestURI)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(e: HttpMessageNotReadableException, request: HttpServletRequest) =
        badRequest(ErrorCode.INVALID_PARAMETER, request.requestURI)

    @ExceptionHandler(Exception::class)
    fun handle(e: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", e)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
                code = ErrorCode.INTERNAL_SERVER_ERROR.code,
                message = ErrorCode.INTERNAL_SERVER_ERROR.message,
                path = request.requestURI
            )
        )
    }

    private fun badRequest(code: ErrorCode, path: String) = ResponseEntity.badRequest().body(
        ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = code.code,
            message = code.message,
            path = path
        )
    )
}
