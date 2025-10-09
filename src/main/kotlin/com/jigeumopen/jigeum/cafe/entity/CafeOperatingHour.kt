package com.jigeumopen.jigeum.cafe.entity

import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = "cafe_operating_hours")
class CafeOperatingHour(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "cafe_id", nullable = false)
    val cafeId: Long,

    @Column(nullable = false)
    val dayOfWeek: Int, // 0 = Sunday, 1 = Monday ...

    @Column(name = "open_time", nullable = false)
    val openTime: LocalTime,

    @Column(name = "close_time", nullable = false)
    val closeTime: LocalTime
)
