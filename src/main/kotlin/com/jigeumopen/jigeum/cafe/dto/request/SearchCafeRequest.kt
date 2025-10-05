package com.jigeumopen.jigeum.cafe.dto.request

import jakarta.validation.constraints.*
import java.time.LocalTime

data class SearchCafeRequest(
    @field:NotNull(message = "위도는 필수입니다")
    @field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    val lat: Double,

    @field:NotNull(message = "경도는 필수입니다")
    @field:DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    val lng: Double,

    @field:Min(value = 100, message = "검색 반경은 100m 이상이어야 합니다")
    @field:Max(value = 50000, message = "검색 반경은 50km 이하여야 합니다")
    val radius: Int = DEFAULT_RADIUS,

    @field:Pattern(
        regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$",
        message = "시간 형식이 올바르지 않습니다 (HH:mm)"
    )
    val time: String = LocalTime.now().toString().substring(0, 5),

    @field:Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    val page: Int = DEFAULT_PAGE_NUMBER,

    @field:Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @field:Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    val size: Int = DEFAULT_PAGE_SIZE
) {
    companion object {
        const val DEFAULT_RADIUS = 1000
        const val DEFAULT_PAGE_NUMBER = 0
        const val DEFAULT_PAGE_SIZE = 20
    }
}
