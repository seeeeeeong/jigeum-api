package com.jigeumopen.jigeum.cafe.entity

import com.jigeumopen.jigeum.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(
    name = "cafe_operating_hours",
    indexes = [
        Index(name = "idx_operating_cafe_day", columnList = "cafe_id,day_of_week")
    ]
)
class CafeOperatingHour(
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int, // 0 = Sunday, 6 = Saturday

    @Column(name = "open_time", nullable = false)
    val openTime: LocalTime,

    @Column(name = "close_time", nullable = false)
    val closeTime: LocalTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafe_id", nullable = false)
    var cafe: Cafe? = null
) : BaseEntity() {

    constructor(
        cafeId: Long,
        dayOfWeek: Int,
        openTime: LocalTime,
        closeTime: LocalTime
    ) : this(dayOfWeek, openTime, closeTime)

    init {
        require(dayOfWeek in 0..6) { "Day of week must be between 0-6" }
    }

    fun isOpenAt(time: LocalTime): Boolean = when {
        closeTime == LocalTime.MIDNIGHT || closeTime < openTime ->
            time >= openTime || time < closeTime
        else -> time in openTime..closeTime
    }
}
