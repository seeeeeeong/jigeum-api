package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.batch.entity.CafeRawData
import com.jigeumopen.jigeum.cafe.dto.CafeDetailResponse
import com.jigeumopen.jigeum.cafe.dto.CafeRequest
import com.jigeumopen.jigeum.cafe.dto.CafeResponse
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.repository.CafeOperatingHourRepository
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.config.recordSuspend
import com.jigeumopen.jigeum.common.dto.PageResponse
import com.jigeumopen.jigeum.common.util.GeohashUtils
import com.jigeumopen.jigeum.common.util.GeometryUtils
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeService(
    private val geometryUtils: GeometryUtils,
    private val geohashUtils: GeohashUtils,
    private val cafeRepository: CafeRepository,
    private val cafeOperatingHourService: CafeOperatingHourService,
    private val cafeOperatingHourRepository: CafeOperatingHourRepository,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val searchCounter: Counter = meterRegistry.counter("cafe.search.count")
    private val searchTimer: Timer = meterRegistry.timer("cafe.search.duration")
    private val detailCounter: Counter = meterRegistry.counter("cafe.detail.count")
    private val detailTimer: Timer = meterRegistry.timer("cafe.detail.duration")

    @Cacheable(
        value = ["nearby"],
        keyGenerator = "geohashKeyGenerator",
        unless = "#result.content.isEmpty()"
    )
    suspend fun searchNearby(request: CafeRequest): PageResponse<CafeResponse> = 
        searchTimer.recordSuspend {
            searchCounter.increment()
            
            withContext(Dispatchers.IO) {
                val time = LocalTime.parse(request.time)
                val dayOfWeek = LocalDate.now().dayOfWeek.value % 7

                val geohash = geohashUtils.encode(request.lat, request.lng, precision = 6)
                val roundedRadius = (request.radius / 100) * 100
                val cacheKey = "$geohash:$roundedRadius:${request.time}"
                
                logger.debug(
                    "Searching cafes - cacheKey: {}, lat: {}, lng: {}, radius: {}, time: {}, day: {}",
                    cacheKey, request.lat, request.lng, request.radius, time, dayOfWeek
                )

                val cafes = cafeRepository.findNearbyOpenCafes(
                    latitude = request.lat,
                    longitude = request.lng,
                    radius = request.radius,
                    dayOfWeek = dayOfWeek,
                    time = time
                )

                logger.info("Found {} cafes within {}m radius (cache key: {})", cafes.size, request.radius, cacheKey)

                meterRegistry.gauge("cafe.search.results", cafes.size)

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

    @Cacheable(value = ["cafeDetail"], key = "#cafeId")
    suspend fun getCafeDetail(cafeId: Long): CafeDetailResponse =
        detailTimer.recordSuspend {
            detailCounter.increment()
            
            withContext(Dispatchers.IO) {
                val cafe = cafeRepository.findById(cafeId)
                    .orElseThrow { 
                        logger.error("Cafe not found: {}", cafeId)
                        IllegalArgumentException("cafe not found: $cafeId") 
                    }

                val operatingHours = cafeOperatingHourRepository
                    .findByPlaceIdOrderByDayOfWeekAsc(cafe.placeId)

                logger.debug("Loaded cafe detail: {} with {} operating hours", cafe.name, operatingHours.size)

                CafeDetailResponse.from(cafe, operatingHours)
            }
        }

    @Transactional
    fun processRawDataToCafe(cafeRawData: CafeRawData) {
        try {
            val cafe = cafeRepository.findByPlaceId(cafeRawData.placeId) ?: run {
                val locationPoint = geometryUtils.createPoint(cafeRawData.longitude, cafeRawData.latitude)
                cafeRepository.save(Cafe.create(cafeRawData, locationPoint))
            }

            cafeRawData.openingHours?.let {
                cafeOperatingHourService.processOperatingHour(cafe, it)
            }

            cafeRawData.updateProcessed()

            logger.debug("Processed cafe: {} ({})", cafe.name, cafe.placeId)
        } catch (e: Exception) {
            logger.error("Failed to process cafe raw data: {}", cafeRawData.placeId, e)
            throw e
        }
    }
}
