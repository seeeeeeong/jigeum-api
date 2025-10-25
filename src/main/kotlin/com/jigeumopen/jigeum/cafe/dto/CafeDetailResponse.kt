package com.jigeumopen.jigeum.cafe.dto

import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.entity.CafeOperatingHour
import com.jigeumopen.jigeum.cafe.entity.DayOfWeek
import java.math.BigDecimal

data class CafeDetailResponse(
    val id: Long?,
    val name: String,
    val address: String?,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val operatingHours: List<OperatingHourDto>
) {
    companion object {
        fun from(cafe: Cafe, operatingHours: List<CafeOperatingHour>) = CafeDetailResponse(
            id = cafe.id,
            name = cafe.name,
            address = cafe.address,
            latitude = cafe.latitude,
            longitude = cafe.longitude,
            operatingHours = operatingHours.map { OperatingHourDto.from(it) }
        )
    }
}

data class OperatingHourDto(
    val dayOfWeek: Int,
    val dayName: String,
    val openTime: String,
    val closeTime: String
) {
    companion object {
        fun from(hour: CafeOperatingHour) = OperatingHourDto(
            dayOfWeek = hour.dayOfWeek,
            dayName = DayOfWeek.fromValue(hour.dayOfWeek).day,
            openTime = hour.openTime.toString(),
            closeTime = hour.closeTime.toString()
        )
    }
}
