package com.jigeumopen.jigeum.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    // 400 Bad Request
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "CAFE-400", "잘못된 파라미터입니다"),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "CAFE-401", "인증이 필요합니다"),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "CAFE-403", "접근 권한이 없습니다"),

    // 429 Too Many Requests
    GOOGLE_API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "CAFE-429", "API 호출 제한 초과"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CAFE-500", "서버 오류가 발생했습니다"),

    // 503 Service Unavailable
    EXTERNAL_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CAFE-503-001", "외부 API 호출 실패"),
    GOOGLE_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CAFE-503-002", "Google API 호출 실패"),
}
