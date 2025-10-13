package com.jigeumopen.jigeum.cafe.entity

import com.jigeumopen.jigeum.batch.entity.GooglePlacesRawData
import com.jigeumopen.jigeum.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import java.math.BigDecimal

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
) : BaseEntity() {
    companion object {
        fun create(rawData: GooglePlacesRawData, location: Point): Cafe = Cafe(
            placeId = rawData.placeId,
            name = rawData.displayName!!,
            address = rawData.formattedAddress,
            latitude = BigDecimal.valueOf(rawData.latitude),
            longitude = BigDecimal.valueOf(rawData.longitude),
            location = location
        )
    }
}
