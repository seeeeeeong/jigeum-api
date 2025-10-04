package com.jigeumopen.jigeum.service

import com.jigeumopen.jigeum.api.dto.request.CreateCafeRequest
import com.jigeumopen.jigeum.api.dto.response.CafeResponse
import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.common.util.GeometryUtils
import com.jigeumopen.jigeum.domain.entity.Cafe
import com.jigeumopen.jigeum.domain.repository.CafeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CafeService(
    private val cafeRepository: CafeRepository,
    private val geometryUtils: GeometryUtils
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAllCafes(): List<CafeResponse> {
        return cafeRepository.findAll()
            .map { CafeResponse.from(it) }
            .also { logger.debug("전체 카페 조회: ${it.size}개") }
    }

    fun getCafe(id: Long): CafeResponse {
        return cafeRepository.findById(id)
            .map { CafeResponse.from(it) }
            .orElseThrow { BusinessException.notFound("카페", id) }
            .also { logger.debug("카페 조회 성공: id=$id") }
    }

    @Transactional
    fun createCafe(request: CreateCafeRequest): CafeResponse {
        logger.info("카페 생성 시작: ${request.name}")

        validateCafeRequest(request)
        checkDuplicateCafe(request.name)

        val cafe = buildCafe(request)
        val savedCafe = cafeRepository.save(cafe)

        logger.info("카페 생성 완료: id=${savedCafe.id}, name=${savedCafe.name}")
        return CafeResponse.from(savedCafe)
    }

    @Transactional
    fun updateCafe(id: Long, request: CreateCafeRequest): CafeResponse {
        logger.info("카페 수정 시작: id=$id")

        val existingCafe = cafeRepository.findById(id)
            .orElseThrow { BusinessException.notFound("카페", id) }

        validateCafeRequest(request)

        // 이름 변경 시 중복 체크
        if (existingCafe.name != request.name) {
            checkDuplicateCafe(request.name)
        }

        val updatedCafe = updateCafeEntity(existingCafe, request)
        val savedCafe = cafeRepository.save(updatedCafe)

        logger.info("카페 수정 완료: id=$id")
        return CafeResponse.from(savedCafe)
    }

    @Transactional
    fun deleteCafe(id: Long) {
        logger.info("카페 삭제 시작: id=$id")

        if (!cafeRepository.existsById(id)) {
            throw BusinessException.notFound("카페", id)
        }

        cafeRepository.deleteById(id)
        logger.info("카페 삭제 완료: id=$id")
    }

    private fun validateCafeRequest(request: CreateCafeRequest) {
        if (request.openTime != null && request.openTime >= request.closeTime) {
            throw BusinessException(
                ErrorCode.INVALID_BUSINESS_HOURS,
                "오픈: ${request.openTime}, 마감: ${request.closeTime}"
            )
        }
    }

    private fun checkDuplicateCafe(name: String) {
        if (cafeRepository.existsByName(name)) {
            throw BusinessException.duplicate("카페", name)
        }
    }

    private fun buildCafe(request: CreateCafeRequest): Cafe {
        return Cafe(
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

    private fun updateCafeEntity(cafe: Cafe, request: CreateCafeRequest): Cafe {
        return Cafe(
            id = cafe.id,
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
}
