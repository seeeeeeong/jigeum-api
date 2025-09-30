package com.jigeumopen.jigeum.controller

import com.jigeumopen.jigeum.domain.Cafe
import com.jigeumopen.jigeum.dto.CafeResponse
import com.jigeumopen.jigeum.repository.CafeRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalTime

@RestController
@RequestMapping("/api/v1/cafes")
class CafeController(
    private val cafeRepository: CafeRepository
) {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @GetMapping
    fun getAllCafes(): List<CafeResponse> {
        return cafeRepository.findAll().map { CafeResponse.from(it) }
    }

    @GetMapping("/{id}")
    fun getCafe(@PathVariable id: Long): ResponseEntity<CafeResponse> {
        return cafeRepository.findById(id)
            .map { ResponseEntity.ok(CafeResponse.from(it)) }
            .orElse(ResponseEntity.notFound().build())
    }

    @PostMapping
    fun createCafe(@RequestBody request: CreateCafeRequest): CafeResponse {
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

data class CreateCafeRequest(
    val name: String,
    val address: String?,
    val phone: String?,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val openTime: LocalTime?,
    val closeTime: LocalTime,
    val category: String?,
    val rating: BigDecimal?
)
