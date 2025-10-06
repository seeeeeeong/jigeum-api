package com.jigeumopen.jigeum.common.exception

import com.jigeumopen.jigeum.cafe.dto.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("[${e.errorCode.code}] ${e.message}", e)
        return ResponseEntity.status(e.status)
            .body(ErrorResponse.of(e, request.requestURI))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map {
            ErrorResponse.FieldError(it.field, it.rejectedValue, it.defaultMessage)
        }

        return ResponseEntity.badRequest().body(
            ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_PARAMETER.message,
                request.requestURI,
                errors
            )
        )
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(
        e: BindException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.map {
            ErrorResponse.FieldError(it.field, it.rejectedValue, it.defaultMessage)
        }

        return ResponseEntity.badRequest().body(
            ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_PARAMETER.message,
                request.requestURI,
                errors
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", e)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.message,
                request.requestURI
            )
        )
    }
}
