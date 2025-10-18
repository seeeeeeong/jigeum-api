package com.jigeumopen.jigeum.common.exception

import com.jigeumopen.jigeum.common.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException, req: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.error("[{}] {}", e.errorCode.code, e.message)
        return ResponseEntity
            .status(e.status)
            .body(
                ErrorResponse(
                code = e.errorCode.code,
                message = e.message ?: "Unknown error",
                path = req.requestURI
            )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse(
                code = "VALIDATION_ERROR",
                message = "입력값이 올바르지 않습니다",
                path = req.requestURI,
                errors = errors
            )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleAll(e: Exception, req: HttpServletRequest): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                code = "INTERNAL_ERROR",
                message = "서버 오류가 발생했습니다",
                path = req.requestURI
            )
            )
    }
}
