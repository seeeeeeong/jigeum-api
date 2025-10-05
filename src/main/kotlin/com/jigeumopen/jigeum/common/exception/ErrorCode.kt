package com.jigeumopen.jigeum.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    // 400 Bad Request
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "CAFE-400-001", "잘못된 파라미터입니다"),
    INVALID_TIME_FORMAT(HttpStatus.BAD_REQUEST, "CAFE-400-002", "시간 형식이 올바르지 않습니다"),
    INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "CAFE-400-003", "유효하지 않은 좌표입니다"),
    INVALID_SEARCH_RADIUS(HttpStatus.BAD_REQUEST, "CAFE-400-004", "검색 반경은 100m ~ 50km 사이여야 합니다"),
    INVALID_PAGING_PARAMETER(HttpStatus.BAD_REQUEST, "CAFE-400-005", "페이징 파라미터가 올바르지 않습니다"),
    INVALID_BUSINESS_HOURS(HttpStatus.BAD_REQUEST, "CAFE-400-006", "오픈 시간은 마감 시간보다 이전이어야 합니다"),
    MISSING_REQUEST_BODY(HttpStatus.BAD_REQUEST, "CAFE-400-007", "요청 본문이 비어있습니다"),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "CAFE-401-001", "인증이 필요합니다"),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "CAFE-401-002", "API 키가 유효하지 않습니다"),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "CAFE-403-001", "접근 권한이 없습니다"),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "CAFE-403-002", "관리자 권한이 필요합니다"),

    // 404 Not Found
    CAFE_NOT_FOUND(HttpStatus.NOT_FOUND, "CAFE-404-001", "카페를 찾을 수 없습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "CAFE-404-002", "요청한 리소스를 찾을 수 없습니다"),

    // 409 Conflict
    DUPLICATE_CAFE(HttpStatus.CONFLICT, "CAFE-409-001", "이미 존재하는 카페입니다"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "CAFE-409-002", "중복된 리소스입니다"),

    // 429 Too Many Requests
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CAFE-429-001", "API 호출 제한을 초과했습니다"),
    GOOGLE_API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "CAFE-429-002", "Google API 호출 제한 초과"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CAFE-500-001", "서버 내부 오류가 발생했습니다"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CAFE-500-002", "데이터베이스 오류가 발생했습니다"),
    SEARCH_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CAFE-500-003", "검색 중 오류가 발생했습니다"),
    SAVE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CAFE-500-004", "저장 중 오류가 발생했습니다"),

    // 503 Service Unavailable
    EXTERNAL_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CAFE-503-001", "외부 API 호출에 실패했습니다"),
    GOOGLE_API_ERROR(
        HttpStatus.SERVICE_UNAVAILABLE,
        "CAFE-503-002",
        "Google Places API 호출에 실패했습니다"
    ),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CAFE-503-003", "서비스를 일시적으로 사용할 수 없습니다");
}
