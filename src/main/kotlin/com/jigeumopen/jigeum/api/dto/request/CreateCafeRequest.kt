package com.jigeumopen.jigeum.api.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalTime

data class CreateCafeRequest(
    @field:NotBlank(message = "카페 이름은 필수입니다")
    @field:Size(min = 1, max = 100, message = "카페 이름은 1~100자 이내여야 합니다")
    val name: String,

    @field:Size(max = 200, message = "주소는 200자 이내여야 합니다")
    val address: String?,

    @field:Pattern(
        regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
        message = "전화번호 형식이 올바르지 않습니다 (예: 02-1234-5678)"
    )
    val phone: String?,

    @field:NotNull(message = "위도는 필수입니다")
    @field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    val latitude: BigDecimal,

    @field:NotNull(message = "경도는 필수입니다")
    @field:DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    val longitude: BigDecimal,

    val openTime: LocalTime?,

    @field:NotNull(message = "마감 시간은 필수입니다")
    val closeTime: LocalTime,

    @field:Size(max = 50, message = "카테고리는 50자 이내여야 합니다")
    val category: String?,

    @field:DecimalMin(value = "0.0", message = "평점은 0 이상이어야 합니다")
    @field:DecimalMax(value = "5.0", message = "평점은 5 이하여야 합니다")
    @field:Digits(integer = 1, fraction = 2, message = "평점은 소수점 2자리까지 가능합니다")
    val rating: BigDecimal?
)
