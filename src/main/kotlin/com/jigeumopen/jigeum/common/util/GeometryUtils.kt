package com.jigeumopen.jigeum.common.util

import com.jigeumopen.jigeum.common.constants.CafeConstants.Location.SRID
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Component

@Component
class GeometryUtils {
    private val geometryFactory = GeometryFactory(PrecisionModel(), SRID)

    fun createPoint(longitude: Double, latitude: Double): Point {
        return geometryFactory.createPoint(Coordinate(longitude, latitude))
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 미터 단위
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}
