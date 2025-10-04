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
    private val repository: CafeRepository,
    private val geometryUtils: GeometryUtils
) {
    fun getAll(): List<CafeResponse> =
        repository.findAll().map { CafeResponse.from(it) }

    fun get(id: Long): CafeResponse =
        repository.findById(id)
            .map { CafeResponse.from(it) }
            .orElseThrow { BusinessException(ErrorCode.CAFE_NOT_FOUND) }

    @Transactional
    fun create(request: CreateCafeRequest): CafeResponse {
        validate(request)
        checkDuplicate(request.name)

        val cafe = buildCafe(request)
        return CafeResponse.from(repository.save(cafe))
    }

    @Transactional
    fun update(id: Long, request: CreateCafeRequest): CafeResponse {
        val existing = repository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.CAFE_NOT_FOUND) }

        validate(request)
        if (existing.name != request.name) checkDuplicate(request.name)

        val updated = buildCafe(request, existing.id)
        return CafeResponse.from(repository.save(updated))
    }

    @Transactional
    fun delete(id: Long) {
        if (!repository.existsById(id)) {
            throw BusinessException(ErrorCode.CAFE_NOT_FOUND)
        }
        repository.deleteById(id)
    }

    private fun validate(request: CreateCafeRequest) {
        if (request.openTime != null && request.openTime >= request.closeTime) {
            throw BusinessException(ErrorCode.INVALID_BUSINESS_HOURS)
        }
    }

    private fun checkDuplicate(name: String) {
        if (repository.existsByName(name)) {
            throw BusinessException(ErrorCode.DUPLICATE_CAFE)
        }
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
