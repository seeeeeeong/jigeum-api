package com.jigeumopen.jigeum.cafe.entity

import com.jigeumopen.jigeum.common.entity.BaseEntity
import jakarta.persistence.*
import org.locationtech.jts.geom.Point
import java.math.BigDecimal

@Entity
@Table(
    name = "cafes",
    indexes = [
        Index(name = "idx_cafe_place_id", columnList = "place_id"),
        Index(name = "idx_cafe_location", columnList = "location")
    ]
)
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
    val location: Point,

    @OneToMany(mappedBy = "cafe", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val operatingHours: MutableList<CafeOperatingHour> = mutableListOf()
) : BaseEntity() {

    fun updateInfo(name: String, address: String?) {
        this.name = name
        this.address = address
    }

    fun addOperatingHours(hours: List<CafeOperatingHour>) {
        operatingHours.addAll(hours)
        hours.forEach { it.cafe = this }
    }
}
