package com.jigeumopen.jigeum.cafe.entity

import com.jigeumopen.jigeum.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "cafe_operating_hours")
class CafeOperatingHour(
    @Column(name = "cafe_id", nullable = false)
    val cafeId: Long,

    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int,

    @Column(name = "open_time", nullable = false)
    val openTime: LocalTime,

    @Column(name = "close_time", nullable = false)
    val closeTime: LocalTime
) : BaseEntity() {

    init {
        require(dayOfWeek in 0..6) { "Day of week must be between 0-6" }
    }

    fun isOpenAt(time: LocalTime): Boolean = when {
        closeTime == LocalTime.MIDNIGHT || closeTime < openTime ->
            time >= openTime || time < closeTime
        else -> time in openTime..closeTime
    }

    companion object {
        fun fromPeriod(cafeId: Long, period: com.jigeumopen.jigeum.cafe.dto.Period): CafeOperatingHour? {
            val open = period.open ?: return null
            val close = period.close ?: return null
            if (open.day == null || open.hour == null || close.hour == null) return null

            return CafeOperatingHour(
                cafeId = cafeId,
                dayOfWeek = open.day,
                openTime = LocalTime.of(open.hour, open.minute ?: 0),
                closeTime = LocalTime.of(close.hour, close.minute ?: 0)
            )
        }
    }
}
