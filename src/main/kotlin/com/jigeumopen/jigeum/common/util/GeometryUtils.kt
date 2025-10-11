package com.jigeumopen.jigeum.common.util

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Component

@Component
class GeometryUtils {
    private val factory = GeometryFactory(PrecisionModel(), 4326)

    fun createPoint(longitude: Double, latitude: Double): Point =
        factory.createPoint(Coordinate(longitude, latitude))
}
