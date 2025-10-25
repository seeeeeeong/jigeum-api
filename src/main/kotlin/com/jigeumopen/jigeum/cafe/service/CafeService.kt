package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.batch.entity.CafeRawData
import com.jigeumopen.jigeum.cafe.dto.CafeDetailResponse
import com.jigeumopen.jigeum.cafe.dto.CafeRequest
import com.jigeumopen.jigeum.cafe.dto.CafeResponse
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.repository.CafeOperatingHourRepository
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.dto.PageResponse
import com.jigeumopen.jigeum.common.util.GeometryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeService(
    private val geometryUtils: GeometryUtils,
    private val cafeRepository: CafeRepository,
    private val cafeOperatingHourService: CafeOperatingHourService,
    private val cafeOperatingHourRepository: CafeOperatingHourRepository
) {
    suspend fun searchNearby(request: CafeRequest): PageResponse<CafeResponse> =
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

    suspend fun getCafeDetail(cafeId: Long): CafeDetailResponse =
        withContext(Dispatchers.IO) {
            val cafe = cafeRepository.findById(cafeId)
                .orElseThrow { IllegalArgumentException("cafe not found: $cafeId") }

            val operatingHours = cafeOperatingHourRepository
                .findByPlaceIdOrderByDayOfWeekAsc(cafe.placeId)

            CafeDetailResponse.from(cafe, operatingHours)
        }

    @Transactional
    fun processRawDataToCafe(cafeRawData: CafeRawData) {
        val cafe = cafeRepository.findByPlaceId(cafeRawData.placeId) ?: run {
            val locationPoint = geometryUtils.createPoint(cafeRawData.longitude, cafeRawData.latitude)
            cafeRepository.save(Cafe.create(cafeRawData, locationPoint))
        }

        cafeRawData.openingHours?.let {
            cafeOperatingHourService.processOperatingHour(cafe, it)
        }
    }
}
