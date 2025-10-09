package com.jigeumopen.jigeum.cafe.entity

import jakarta.persistence.*
import org.locationtech.jts.geom.Point
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "cafes")
class Cafe(
    @Column(name = "place_id", nullable = false, unique = true, length = 100)
    val placeId: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 200)
    var address: String? = null,

    @Column(nullable = false, precision = 10, scale = 8)
    val latitude: BigDecimal,

    @Column(nullable = false, precision = 11, scale = 8)
    val longitude: BigDecimal,

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    val location: Point
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    fun updateInfo(name: String, address: String?) {
        this.name = name
        this.address = address
        this.updatedAt = LocalDateTime.now()
    }
}
