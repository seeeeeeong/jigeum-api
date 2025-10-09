package com.jigeumopen.jigeum.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "CAFE-400-001", "잘못된 파라미터입니다"),
    GOOGLE_API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "CAFE-429-002", "Google API 호출 제한 초과"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CAFE-500-001", "서버 내부 오류가 발생했습니다"),
    EXTERNAL_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CAFE-503-001", "외부 API 호출에 실패했습니다"),
    GOOGLE_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CAFE-503-002", "Google Places API 호출에 실패했습니다"),
}
