package com.jigeumopen.jigeum.cafe.dto.response

import com.jigeumopen.jigeum.cafe.entity.Cafe
import java.math.BigDecimal

data class CafeResponse(
    val id: Long?,
    val name: String,
    val address: String?,
    val latitude: BigDecimal,
    val longitude: BigDecimal
) {
    companion object {
        fun from(cafe: Cafe) = CafeResponse(
            id = cafe.id,
            name = cafe.name,
            address = cafe.address,
            latitude = cafe.latitude,
            longitude = cafe.longitude
        )
    }
}
