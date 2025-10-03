package com.jigeumopen.jigeum.service

import com.jigeumopen.jigeum.domain.entity.Cafe
import com.jigeumopen.jigeum.api.dto.response.CafeResponse
import com.jigeumopen.jigeum.api.dto.request.CreateCafeRequest
import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.domain.repository.CafeRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeService(
    private val cafeRepository: CafeRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun getAllCafes(): List<CafeResponse> {
        val cafes = cafeRepository.findAll()
        logger.debug("전체 카페 조회: ${cafes.size}개")
        return cafes.map { CafeResponse.from(it) }
    }

    fun getCafe(id: Long): CafeResponse {
        logger.debug("카페 단건 조회: id=$id")

        val cafe = cafeRepository.findById(id)
            .orElseThrow {
                BusinessException.notFound("카페", id)
            }

        return CafeResponse.from(cafe)
    }

    fun searchCafes(
        latitude: Double,
        longitude: Double,
        radius: Int,
        requiredTime: LocalTime,
        page: Int = 0,
        size: Int = 20
    ): List<CafeResponse> {
        logger.debug("카페 검색 - lat: $latitude, lng: $longitude, radius: $radius, time: $requiredTime")

        if (radius < 100 || radius > 50000) {
            throw BusinessException(
                ErrorCode.INVALID_SEARCH_RADIUS,
                "입력값: ${radius}m"
            )
        }

        if (page < 0 || size < 1 || size > 100) {
            throw BusinessException(
                ErrorCode.INVALID_PAGING_PARAMETER,
                "page: $page, size: $size"
            )
        }

        val offset = page * size

        val cafes = try {
            cafeRepository.findNearbyOpenCafes(
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                requiredTime = requiredTime,
                limit = size,
                offset = offset
            )
        } catch (e: Exception) {
            logger.error("카페 검색 중 오류 발생", e)
            throw BusinessException(
                ErrorCode.SEARCH_ERROR,
                e.message ?: "알 수 없는 오류",
                e
            )
        }

        logger.debug("검색 결과: ${cafes.size}개")
        return cafes.map { CafeResponse.from(it) }
    }

    @Transactional
    fun createCafe(request: CreateCafeRequest): CafeResponse {
        logger.info("카페 생성: ${request.name}")

        if (cafeRepository.existsByName(request.name)) {
            throw BusinessException.duplicate("카페", request.name)
        }

        if (request.openTime != null && request.openTime >= request.closeTime) {
            throw BusinessException(
                ErrorCode.INVALID_BUSINESS_HOURS,
                "오픈: ${request.openTime}, 마감: ${request.closeTime}"
            )
        }

        val point = try {
            geometryFactory.createPoint(
                Coordinate(request.longitude.toDouble(), request.latitude.toDouble())
            )
        } catch (e: Exception) {
            logger.error("좌표 생성 실패", e)
            throw BusinessException(
                ErrorCode.INVALID_COORDINATE,
                "lat: ${request.latitude}, lng: ${request.longitude}"
            )
        }

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

        val savedCafe = try {
            cafeRepository.save(cafe)
        } catch (e: Exception) {
            logger.error("카페 저장 실패", e)
            throw BusinessException(
                ErrorCode.SAVE_ERROR,
                e.message ?: "데이터베이스 오류",
                e
            )
        }

        logger.info("카페 생성 완료: id=${savedCafe.id}, name=${savedCafe.name}")
        return CafeResponse.from(savedCafe)
    }
}
