package com.jigeumopen.jigeum.cafe.entity

import jakarta.persistence.*
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
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    init {
        require(dayOfWeek in 0..6) { "Day of week must be between 0-6" }
    }

    fun isOpenAt(time: LocalTime): Boolean {
        return if (closeTime == LocalTime.MIDNIGHT || closeTime < openTime) {
            time >= openTime || time < closeTime
        } else {
            time in openTime..closeTime
        }
    }

    companion object {
        fun of(cafeId: Long, dayOfWeek: Int, openTime: LocalTime, closeTime: LocalTime): CafeOperatingHour {
            return CafeOperatingHour(cafeId, dayOfWeek, openTime, closeTime)
        }
    }
}
