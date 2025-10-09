package com.jigeumopen.jigeum.common.exception

import com.jigeumopen.jigeum.cafe.dto.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("[{}] Business error: {}", e.errorCode.code, e.message)

        return ResponseEntity
            .status(e.status)
            .body(ErrorResponse.of(e, request.requestURI))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map { error ->
            ErrorResponse.FieldError(
                field = error.field,
                value = error.rejectedValue,
                message = error.defaultMessage
            )
        }

        logger.warn("Validation failed: {}", errors)

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.of(
                    status = HttpStatus.BAD_REQUEST,
                    message = "입력 값이 올바르지 않습니다",
                    path = request.requestURI,
                    errors = errors
                )
            )
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(
        e: BindException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map { error ->
            ErrorResponse.FieldError(
                field = error.field,
                value = error.rejectedValue,
                message = error.defaultMessage
            )
        }

        logger.warn("Binding failed: {}", errors)

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.of(
                    status = HttpStatus.BAD_REQUEST,
                    message = "입력 값이 올바르지 않습니다",
                    path = request.requestURI,
                    errors = errors
                )
            )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        e: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = e.constraintViolations.map { violation ->
            ErrorResponse.FieldError(
                field = violation.propertyPath.toString(),
                value = violation.invalidValue,
                message = violation.message
            )
        }

        logger.warn("Constraint violation: {}", errors)

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.of(
                    status = HttpStatus.BAD_REQUEST,
                    message = "입력 제약 조건 위반",
                    path = request.requestURI,
                    errors = errors
                )
            )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        e: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse.FieldError(
            field = e.name,
            value = e.value,
            message = "타입이 올바르지 않습니다"
        )

        logger.warn("Type mismatch: {}", error)

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.of(
                    status = HttpStatus.BAD_REQUEST,
                    message = "파라미터 타입이 올바르지 않습니다",
                    path = request.requestURI,
                    errors = listOf(error)
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        e: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: {}", e.message)

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.of(
                    status = HttpStatus.BAD_REQUEST,
                    message = e.message ?: "잘못된 요청입니다",
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", e)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse.of(
                    status = HttpStatus.INTERNAL_SERVER_ERROR,
                    message = "서버 내부 오류가 발생했습니다",
                    path = request.requestURI
                )
            )
    }
}
