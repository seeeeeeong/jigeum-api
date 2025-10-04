package com.jigeumopen.jigeum.api.exception

import org.springframework.http.HttpStatus

class BusinessException(
    val errorCode: ErrorCode,
    val detail: String? = null,
    cause: Throwable? = null
) : RuntimeException(errorCode.message, cause) {

    val status: HttpStatus = errorCode.status

    constructor(errorCode: ErrorCode) : this(errorCode, null, null)

    constructor(errorCode: ErrorCode, cause: Throwable) : this(errorCode, null, cause)
}
