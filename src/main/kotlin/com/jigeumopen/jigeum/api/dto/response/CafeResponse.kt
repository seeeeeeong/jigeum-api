package com.jigeumopen.jigeum.api.dto.response

import com.jigeumopen.jigeum.domain.entity.Cafe
import java.math.BigDecimal
import java.time.LocalTime

data class CafeResponse(
    val id: Long?,
    val name: String,
    val address: String?,
    val phone: String?,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val openTime: LocalTime?,
    val closeTime: LocalTime,
    val category: String?,
    val rating: BigDecimal?
) {
    companion object {
        fun from(cafe: Cafe): CafeResponse {
            return CafeResponse(
                id = cafe.id,
                name = cafe.name,
                address = cafe.address,
                phone = cafe.phone,
                latitude = cafe.latitude,
                longitude = cafe.longitude,
                openTime = cafe.openTime,
                closeTime = cafe.closeTime,
                category = cafe.category,
                rating = cafe.rating
            )
        }
    }
}
