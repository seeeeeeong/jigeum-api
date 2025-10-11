package com.jigeumopen.jigeum.cafe.dto.response

import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val code: String,
    val message: String,
    val path: String? = null,
    val errors: Map<String, String>? = null
)
