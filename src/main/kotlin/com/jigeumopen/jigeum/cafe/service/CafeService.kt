package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.dto.CafeResponse
import com.jigeumopen.jigeum.cafe.dto.PageResponse
import com.jigeumopen.jigeum.cafe.dto.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeService(
    private val cafeRepository: CafeRepository
) {
    suspend fun searchNearby(request: SearchCafeRequest): PageResponse<CafeResponse> =
        withContext(Dispatchers.IO) {
            val time = LocalTime.parse(request.time)
            val dayOfWeek = LocalDate.now().dayOfWeek.value % 7

            val cafes = cafeRepository.findNearbyOpenCafes(
                latitude = request.lat,
                longitude = request.lng,
                radius = request.radius,
                dayOfWeek = dayOfWeek,
                time = time
            )

            val paged = cafes
                .drop(request.page * request.size)
                .take(request.size)
                .map { CafeResponse.from(it) }

            PageResponse(
                content = paged,
                page = request.page,
                size = request.size,
                totalElements = cafes.size.toLong(),
                totalPages = (cafes.size + request.size - 1) / request.size
            )
        }
}
