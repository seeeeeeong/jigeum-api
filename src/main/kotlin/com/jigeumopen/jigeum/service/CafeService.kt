package com.jigeumopen.jigeum.service

import com.jigeumopen.jigeum.api.dto.request.CreateCafeRequest
import com.jigeumopen.jigeum.api.dto.response.CafeResponse
import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.common.util.GeometryUtils
import com.jigeumopen.jigeum.domain.entity.Cafe
import com.jigeumopen.jigeum.domain.repository.CafeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CafeService(
    private val cafeRepository: CafeRepository,
    private val geometryUtils: GeometryUtils
) {
    fun getAll(): List<CafeResponse> =
        cafeRepository.findAll().map { CafeResponse.from(it) }

    fun get(id: Long): CafeResponse =
        cafeRepository.findById(id)
            .map { CafeResponse.from(it) }
            .orElseThrow { BusinessException(ErrorCode.CAFE_NOT_FOUND) }

    @Transactional
    fun create(request: CreateCafeRequest): CafeResponse {
        val cafe = buildCafe(request)
        return CafeResponse.from(cafeRepository.save(cafe))
    }

    @Transactional
    fun update(id: Long, request: CreateCafeRequest): CafeResponse {
        val cafe = cafeRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.CAFE_NOT_FOUND) }

        val updated = buildCafe(request, cafe.id)
        return CafeResponse.from(cafeRepository.save(updated))
    }

    @Transactional
    fun delete(id: Long) {
        cafeRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.CAFE_NOT_FOUND) }

        cafeRepository.deleteById(id)
    }
    
    private fun buildCafe(request: CreateCafeRequest, id: Long? = null) = Cafe(
        id = id,
        name = request.name,
        address = request.address,
        phone = request.phone,
        latitude = request.latitude,
        longitude = request.longitude,
        location = geometryUtils.createPoint(
            request.longitude.toDouble(),
            request.latitude.toDouble()
        ),
        openTime = request.openTime,
        closeTime = request.closeTime,
        category = request.category,
        rating = request.rating
    )
}
