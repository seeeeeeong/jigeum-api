package com.jigeumopen.jigeum.common.exception

import com.jigeumopen.jigeum.cafe.dto.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException, req: HttpServletRequest) =
        ResponseEntity.status(e.status)
            .body(ErrorResponse.of(e, req.requestURI))
            .also { log.error("[{}] {}", e.errorCode.code, e.message) }

    @ExceptionHandler(MethodArgumentNotValidException::class, ConstraintViolationException::class)
    fun handleValidation(e: Exception, req: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errors = when (e) {
            is MethodArgumentNotValidException -> e.bindingResult.fieldErrors.map {
                ErrorResponse.FieldError(it.field, it.rejectedValue, it.defaultMessage)
            }
            is ConstraintViolationException -> e.constraintViolations.map {
                ErrorResponse.FieldError(it.propertyPath.toString(), it.invalidValue, it.message)
            }
            else -> emptyList()
        }

        return ResponseEntity.badRequest().body(
            ErrorResponse.of(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다", req.requestURI, errors)
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleAll(e: Exception, req: HttpServletRequest) =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류", req.requestURI))
            .also { log.error("Unexpected error", e) }
}
