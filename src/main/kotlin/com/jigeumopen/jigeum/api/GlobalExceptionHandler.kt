package com.jigeumopen.jigeum.api

import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.api.dto.response.ErrorResponse
import com.jigeumopen.jigeum.external.exception.GooglePlacesException
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
import org.springframework.web.servlet.NoHandlerFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // 비즈니스 로직 예외
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("[${e.errorCode.code}] ${e.message}", e)

        val errorResponse = ErrorResponse.of(e, request.requestURI)
        return ResponseEntity.status(e.status).body(errorResponse)
    }

    // Google Places API 예외
    @ExceptionHandler(GooglePlacesException::class)
    fun handleGooglePlacesException(
        e: GooglePlacesException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Google Places API exception: ${e.message}", e)

        val (status, errorCode) = when (e) {
            is GooglePlacesException.BadRequest -> HttpStatus.BAD_REQUEST to ErrorCode.INVALID_PARAMETER
            is GooglePlacesException.Unauthorized -> HttpStatus.UNAUTHORIZED to ErrorCode.INVALID_API_KEY
            is GooglePlacesException.NotFound -> HttpStatus.NOT_FOUND to ErrorCode.RESOURCE_NOT_FOUND
            is GooglePlacesException.RateLimited -> HttpStatus.TOO_MANY_REQUESTS to ErrorCode.GOOGLE_API_RATE_LIMIT
            is GooglePlacesException.ServerError -> HttpStatus.BAD_GATEWAY to ErrorCode.GOOGLE_API_ERROR
            is GooglePlacesException.UnknownError -> HttpStatus.INTERNAL_SERVER_ERROR to ErrorCode.EXTERNAL_API_ERROR
        }

        val errorResponse = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            code = errorCode.code,
            message = errorCode.message,
            detail = e.message,
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(errorResponse)
    }

    // Validation 예외 - @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation failed: ${e.message}")

        val errors = e.bindingResult.fieldErrors.map { fieldError ->
            ErrorResponse.FieldError(
                field = fieldError.field,
                value = fieldError.rejectedValue,
                message = fieldError.defaultMessage
            )
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = ErrorCode.INVALID_PARAMETER.code,
            message = "입력값 검증 실패",
            path = request.requestURI,
            errors = errors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    // BindException - Form 데이터 바인딩 실패
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        e: BindException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Binding failed: ${e.message}")

        val errors = e.bindingResult.fieldErrors.map { fieldError ->
            ErrorResponse.FieldError(
                field = fieldError.field,
                value = fieldError.rejectedValue,
                message = fieldError.defaultMessage
            )
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = ErrorCode.INVALID_PARAMETER.code,
            message = "요청 파라미터 바인딩 실패",
            path = request.requestURI,
            errors = errors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    // 타입 불일치 예외
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Type mismatch: parameter=${e.name}, value=${e.value}, required=${e.requiredType}")

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = ErrorCode.INVALID_PARAMETER.code,
            message = "파라미터 타입이 일치하지 않습니다",
            detail = "${e.name} 파라미터는 ${e.requiredType?.simpleName} 타입이어야 합니다",
            path = request.requestURI
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    // 필수 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        e: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Missing parameter: ${e.parameterName}")

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = ErrorCode.INVALID_PARAMETER.code,
            message = "필수 파라미터가 누락되었습니다",
            detail = "${e.parameterName} 파라미터가 필요합니다",
            path = request.requestURI
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    // JSON 파싱 실패
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Message not readable: ${e.message}")

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            code = ErrorCode.INVALID_PARAMETER.code,
            message = "요청 본문을 읽을 수 없습니다",
            detail = "JSON 형식이 올바른지 확인해주세요",
            path = request.requestURI
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    // 404 Not Found
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(
        e: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("No handler found: ${request.requestURI}")

        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            code = ErrorCode.RESOURCE_NOT_FOUND.code,
            message = "요청한 리소스를 찾을 수 없습니다",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    // 모든 예외의 최종 처리
    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", e)

        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = "서버 내부 오류가 발생했습니다",
            detail = if (isDebugMode()) e.message else null,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    private fun isDebugMode(): Boolean {
        return System.getProperty("spring.profiles.active") == "dev"
    }
}
