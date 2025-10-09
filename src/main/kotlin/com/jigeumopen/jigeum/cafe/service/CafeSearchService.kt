package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.CafeResponse
import com.jigeumopen.jigeum.cafe.dto.response.PageResponse
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeSearchService(
    private val repository: CafeRepository
) {
    fun searchNearby(request: SearchCafeRequest): PageResponse<CafeResponse> {
        val time = LocalTime.parse(request.time)
        val offset = request.page * request.size

        val cafes = repository.findNearbyOpenCafes(
            latitude = request.lat,
            longitude = request.lng,
            radius = request.radius,
            time = time,
            limit = request.size,
            offset = offset
        )

        return PageResponse(
            content = cafes.map { CafeResponse.from(it) },
            page = request.page,
            size = request.size
        )
    }
}
