package com.jigeumopen.jigeum.common.util

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Component

@Component
class GeometryUtils {
    companion object {
        private const val SRID = 4326
        private val FACTORY = GeometryFactory(PrecisionModel(), SRID)
    }

    fun createPoint(longitude: Double, latitude: Double): Point =
        FACTORY.createPoint(Coordinate(longitude, latitude))

}
