package com.jigeumopen.jigeum.domain

import jakarta.persistence.*
import org.locationtech.jts.geom.Point
import java.math.BigDecimal
import java.time.LocalTime

@Entity
@Table(name = "cafes")
class Cafe(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(length = 200)
    val address: String? = null,

    @Column(length = 20)
    val phone: String? = null,

    @Column(nullable = false, precision = 10, scale = 8)  // NOT NULL 추가!
    val latitude: BigDecimal,

    @Column(nullable = false, precision = 11, scale = 8)  // NOT NULL 추가!
    val longitude: BigDecimal,

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")  // NOT NULL 추가!
    val location: Point,

    @Column(name = "open_time")
    val openTime: LocalTime? = null,

    @Column(name = "close_time", nullable = false)  // NOT NULL 추가!
    val closeTime: LocalTime,

    @Column(length = 50)
    val category: String? = null,

    @Column(precision = 3, scale = 2)
    val rating: BigDecimal? = null
)
