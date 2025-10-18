package com.jigeumopen.jigeum.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T) = ApiResponse(
            success = true,
            data = data
        )

        fun error(message: String) = ApiResponse<Nothing>(
            success = false,
            message = message
        )
    }
}

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
