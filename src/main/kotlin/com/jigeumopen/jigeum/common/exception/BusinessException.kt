package com.jigeumopen.jigeum.common.exception

import org.springframework.http.HttpStatus

data class BusinessException(
    val errorCode: ErrorCode,
    val detail: String? = null,
    override val cause: Throwable? = null
) : RuntimeException(errorCode.message, cause) {
    val status: HttpStatus = errorCode.status
}
