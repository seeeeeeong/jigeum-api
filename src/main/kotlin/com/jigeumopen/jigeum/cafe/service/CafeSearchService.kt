package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.CafeResponse
import com.jigeumopen.jigeum.cafe.dto.response.PageResponse
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeSearchService(
    private val cafeRepository: CafeRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchNearby(request: SearchCafeRequest): PageResponse<CafeResponse> {
        val searchTime = LocalTime.parse(request.time)
        val dayOfWeek = LocalDate.now().dayOfWeek.value % 7
        val offset = request.page * request.size

        logger.debug(
            "Searching cafes - lat: {}, lng: {}, radius: {}m, time: {}, day: {}",
            request.lat, request.lng, request.radius, searchTime, dayOfWeek
        )

        val cafes = cafeRepository.findNearbyOpenCafes(
            latitude = request.lat,
            longitude = request.lng,
            radius = request.radius,
            dayOfWeek = dayOfWeek,
            time = searchTime,
            limit = request.size,
            offset = offset
        )

        val totalElements = if (request.page == 0 && cafes.size < request.size) {
            cafes.size.toLong()
        } else {
            cafeRepository.countNearbyOpenCafes(
                latitude = request.lat,
                longitude = request.lng,
                radius = request.radius,
                dayOfWeek = dayOfWeek,
                time = searchTime
            )
        }

        val responses = cafes.map { CafeResponse.from(it) }

        return PageResponse(
            content = responses,
            page = request.page,
            size = request.size,
            totalElements = totalElements,
            totalPages = ((totalElements + request.size - 1) / request.size).toInt()
        )
    }

}
