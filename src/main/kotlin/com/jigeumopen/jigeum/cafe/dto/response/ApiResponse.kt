package com.jigeumopen.jigeum.cafe.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val metadata: Map<String, Any>? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                message = message
            )
        }

        fun <T> success(data: T, metadata: Map<String, Any>): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                metadata = metadata
            )
        }

        fun error(message: String): ApiResponse<Nothing> {
            return ApiResponse(
                success = false,
                message = message
            )
        }
    }
}

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long? = null,
    val totalPages: Int? = null
)
