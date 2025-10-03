package com.jigeumopen.jigeum.service

import com.jigeumopen.jigeum.domain.Cafe
import com.jigeumopen.jigeum.dto.CafeResponse
import com.jigeumopen.jigeum.dto.CreateCafeRequest
import com.jigeumopen.jigeum.repository.CafeRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeService(
    private val cafeRepository: CafeRepository
) {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun getAllCafes(): List<CafeResponse> =
        cafeRepository.findAll().map { CafeResponse.from(it) }

    fun getCafe(id: Long): CafeResponse =
        cafeRepository.findById(id)
            .map { CafeResponse.from(it) }
            .orElseThrow { NoSuchElementException("카페를 찾을 수 없습니다. id: $id") }

    fun searchCafes(
        latitude: Double,
        longitude: Double,
        radius: Int,
        requiredTime: LocalTime,
        page: Int = 0,
        size: Int = 20
    ): List<CafeResponse> {
        val offset = page * size

        return cafeRepository.findNearbyOpenCafes(
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            requiredTime = requiredTime,
            limit = size,
            offset = offset
        ).map { CafeResponse.from(it) }
    }

    @Transactional
    fun createCafe(request: CreateCafeRequest): CafeResponse {
        val point = geometryFactory.createPoint(
            Coordinate(request.longitude.toDouble(), request.latitude.toDouble())
        )

        val cafe = Cafe(
            name = request.name,
            address = request.address,
            phone = request.phone,
            latitude = request.latitude,
            longitude = request.longitude,
            location = point,
            openTime = request.openTime,
            closeTime = request.closeTime,
            category = request.category,
            rating = request.rating
        )

        return CafeResponse.from(cafeRepository.save(cafe))
    }
}
