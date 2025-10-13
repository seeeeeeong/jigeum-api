package com.jigeumopen.jigeum.cafe.entity

import com.jigeumopen.jigeum.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "cafe_operating_hours")
class CafeOperatingHour(
    @Column(name = "place_id", nullable = false, length = 100)
    val placeId: String,

    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int,

    @Column(name = "open_close", nullable = false, columnDefinition = "tsrange")
    val openClose: String
) : BaseEntity() {

    companion object {
        fun fromPeriod(placeId: String, period: com.jigeumopen.jigeum.cafe.dto.Period): CafeOperatingHour? {
            val open = period.open ?: return null
            val close = period.close ?: return null

            if (open.day == null || open.hour == null || close.hour == null) return null

            val openTime = LocalTime.of(open.hour, open.minute ?: 0)
            val closeTime = LocalTime.of(close.hour, close.minute ?: 0)

            val tsRange = "[$openTime, $closeTime)"

            return CafeOperatingHour(
                placeId = placeId,
                dayOfWeek = open.day,
                openClose = tsRange
            )
        }
    }
}
