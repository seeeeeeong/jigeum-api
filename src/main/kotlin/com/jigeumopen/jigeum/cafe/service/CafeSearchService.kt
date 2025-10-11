package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.CafeResponse
import com.jigeumopen.jigeum.cafe.dto.response.PageResponse
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeSearchService(
    private val cafeRepository: CafeRepository
) {
    fun searchNearby(request: SearchCafeRequest): PageResponse<CafeResponse> {
        val time = LocalTime.parse(request.time)
        val dayOfWeek = LocalDate.now().dayOfWeek.value % 7
        val offset = request.page * request.size

        val cafes = cafeRepository.findNearbyOpenCafes(
            latitude = request.lat,
            longitude = request.lng,
            radius = request.radius,
            dayOfWeek = dayOfWeek,
            time = time,
            limit = request.size,
            offset = offset
        )

        val total = if (cafes.size < request.size) {
            (offset + cafes.size).toLong()
        } else {
            cafeRepository.countNearbyOpenCafes(request.lat, request.lng, request.radius, dayOfWeek, time)
        }

        return PageResponse(
            content = cafes.map { CafeResponse.from(it) },
            page = request.page,
            size = request.size,
            totalElements = total,
            totalPages = ((total + request.size - 1) / request.size).toInt()
        )
    }
}
