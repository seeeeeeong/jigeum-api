package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.CafeResponse
import com.jigeumopen.jigeum.cafe.dto.response.PageResponse
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.format.DateTimeParseException

@Service
@Transactional(readOnly = true)
class CafeSearchService(
    private val repository: CafeRepository
) {
    fun searchNearby(request: SearchCafeRequest): PageResponse<CafeResponse> {
        val time = parseTime(request.time)
        val offset = request.page * request.size

        val cafes = repository.findNearbyOpenCafes(
            latitude = request.lat,
            longitude = request.lng,
            radius = request.radius,
            requiredTime = time,
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
        val pageable = PageRequest.of(page, size)
        val cafes = repository.findByNameContainingIgnoreCase(keyword, pageable)

        return PageResponse(
            content = cafes.content.map { CafeResponse.from(it) },
            page = page,
            size = size,
            totalElements = cafes.totalElements,
            totalPages = cafes.totalPages
        )
    }

    fun searchByCategory(category: String, page: Int, size: Int): PageResponse<CafeResponse> {
        val pageable = PageRequest.of(page, size)
        val cafes = repository.findByCategory(category, pageable)

        return PageResponse(
            content = cafes.content.map { CafeResponse.from(it) },
            page = page,
            size = size,
            totalElements = cafes.totalElements,
            totalPages = cafes.totalPages
        )
    }


    private fun parseTime(timeStr: String): LocalTime {
        return try {
            LocalTime.parse(timeStr)
        } catch (e: DateTimeParseException) {
            throw BusinessException(ErrorCode.INVALID_TIME_FORMAT, e)
        }
    }
}
