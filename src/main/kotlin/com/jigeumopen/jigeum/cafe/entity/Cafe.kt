package com.jigeumopen.jigeum.cafe.entity

import jakarta.persistence.*
import org.locationtech.jts.geom.Point
import java.math.BigDecimal

@Entity
@Table(name = "cafes")
class Cafe(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "place_id", nullable = false, unique = true, length = 100)
    val placeId: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(length = 200)
    val address: String? = null,

    @Column(nullable = false, precision = 10, scale = 8)
    val latitude: BigDecimal,

    @Column(nullable = false, precision = 11, scale = 8)
    val longitude: BigDecimal,

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    val location: Point
)
