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

    @Column(name = "open_time", nullable = false)
    val openTime: LocalTime,

    @Column(name = "close_time", nullable = false)
    val closeTime: LocalTime

) : BaseEntity() {

    companion object {
        fun create(placeId: String, dayOfWeek: Int, openTime: LocalTime, closeTime: LocalTime): CafeOperatingHour {
            return CafeOperatingHour(placeId, dayOfWeek, openTime, closeTime)
        }
    }
}
