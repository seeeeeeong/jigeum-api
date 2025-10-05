package com.jigeumopen.jigeum.cafe.service.mapper

import com.jigeumopen.jigeum.cafe.dto.request.CreateCafeRequest
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.common.util.GeometryUtils
import org.springframework.stereotype.Component

@Component
class CafeMapper(
    private val geometryUtils: GeometryUtils
) {
    fun toEntity(request: CreateCafeRequest, id: Long? = null): Cafe {
        return Cafe(
            id = id,
            name = request.name,
            address = request.address,
            phone = request.phone,
            latitude = request.latitude,
            longitude = request.longitude,
            location = geometryUtils.createPoint(
                longitude = request.longitude.toDouble(),
                latitude = request.latitude.toDouble()
            ),
            openTime = request.openTime,
            closeTime = request.closeTime,
            category = request.category,
            rating = request.rating
        )
    }
}
