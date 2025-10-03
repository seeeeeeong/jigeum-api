package com.jigeumopen.jigeum.service

import com.jigeumopen.jigeum.api.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.api.dto.response.CafeResponse
import com.jigeumopen.jigeum.api.dto.response.PageResponse
import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.common.constants.CafeConstants.Search
import com.jigeumopen.jigeum.domain.repository.CafeRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.format.DateTimeParseException

@Service
@Transactional(readOnly = true)
class CafeSearchService(
    private val cafeRepository: CafeRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchNearbyOpenCafes(request: SearchCafeRequest): PageResponse<CafeResponse> {
        validateSearchRequest(request)

        val requiredTime = parseTime(request.time)
        val offset = request.page * request.size

        logger.debug("검색 파라미터: lat=${request.lat}, lng=${request.lng}, " +
                "radius=${request.radius}, time=$requiredTime, page=${request.page}")

        val cafes = cafeRepository.findNearbyOpenCafes(
            latitude = request.lat,
            longitude = request.lng,
            radius = request.radius,
            requiredTime = requiredTime,
            limit = request.size,
            offset = offset
        )

        return PageResponse(
            content = cafes.map { CafeResponse.from(it) },
            page = request.page,
            size = request.size
        )
    }

    fun searchByName(keyword: String, page: Int, size: Int): PageResponse<CafeResponse> {
        validatePageRequest(page, size)

        val pageable = PageRequest.of(page, size)
        val cafes = cafeRepository.findByNameContainingIgnoreCase(keyword, pageable)

        return PageResponse(
            content = cafes.content.map { CafeResponse.from(it) },
            page = page,
            size = size,
            totalElements = cafes.totalElements,
            totalPages = cafes.totalPages
        )
    }

    fun searchByCategory(category: String, page: Int, size: Int): PageResponse<CafeResponse> {
        validatePageRequest(page, size)

        val pageable = PageRequest.of(page, size)
        val cafes = cafeRepository.findByCategory(category, pageable)

        return PageResponse(
            content = cafes.content.map { CafeResponse.from(it) },
            page = page,
            size = size,
            totalElements = cafes.totalElements,
            totalPages = cafes.totalPages
        )
    }

    private fun validateSearchRequest(request: SearchCafeRequest) {
        if (request.radius !in Search.MIN_RADIUS_METERS..Search.MAX_RADIUS_METERS) {
            throw BusinessException(
                ErrorCode.INVALID_SEARCH_RADIUS,
                "입력값: ${request.radius}m"
            )
        }
        validatePageRequest(request.page, request.size)
    }

    private fun validatePageRequest(page: Int, size: Int) {
        if (page < 0 || size !in Search.MIN_PAGE_SIZE..Search.MAX_PAGE_SIZE) {
            throw BusinessException(
                ErrorCode.INVALID_PAGING_PARAMETER,
                "page: $page, size: $size"
            )
        }
    }

    private fun parseTime(timeStr: String): LocalTime {
        return try {
            LocalTime.parse(timeStr)
        } catch (e: DateTimeParseException) {
            throw BusinessException(
                ErrorCode.INVALID_TIME_FORMAT,
                "입력값: $timeStr"
            )
        }
    }
}
