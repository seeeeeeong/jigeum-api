package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.client.GooglePlacesClient
import com.jigeumopen.jigeum.cafe.dto.Place
import com.jigeumopen.jigeum.cafe.dto.response.AreaCollectionResponse
import com.jigeumopen.jigeum.cafe.dto.response.BatchCollectionResponse
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.entity.CafeOperatingHour
import com.jigeumopen.jigeum.cafe.repository.CafeOperatingHourRepository
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.constants.CafeConstants.SeoulLocations
import com.jigeumopen.jigeum.common.util.GeometryUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Service
class DataCollectionService(
    private val googlePlacesClient: GooglePlacesClient,
    private val cafeRepository: CafeRepository,
    private val cafeOperatingHourRepository: CafeOperatingHourRepository,
    private val geometryUtils: GeometryUtils
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun collectCafesInArea(lat: Double, lng: Double, radius: Int): AreaCollectionResponse {
        val nearbyPlaces = googlePlacesClient.searchNearbyCafes(lat, lng, radius.toDouble()).places.orEmpty()
        val savablePlaces = nearbyPlaces.filter { it.isSavable() }

        if (savablePlaces.isEmpty()) {
            logger.info("No cafes found at ($lat,$lng)")
            return AreaCollectionResponse(0, "$lat,$lng", radius)
        }

        val savedCount = saveNewCafes(savablePlaces)
        logger.info("Collected $savedCount new cafes at ($lat,$lng)")
        return AreaCollectionResponse(savedCount, "$lat,$lng", radius)
    }

    fun collectAllCafes(): BatchCollectionResponse {
        val results = ConcurrentHashMap<String, Int>()

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = SeoulLocations.GRID_POINTS.map { location ->
                executor.submit {
                    val count = runCatching {
                        Thread.sleep(2000) // API rate limit 고려
                        collectCafesInArea(location.latitude, location.longitude, 3000).savedCount
                    }.getOrElse {
                        logger.warn("Failed to collect cafes for ${location.name}: ${it.message}")
                        0
                    }
                    results[location.name] = count
                }
            }
            futures.forEach { it.get() }
        }

        val totalCount = results.values.sum()
        return BatchCollectionResponse(results, totalCount, results.size)
    }

    private fun saveNewCafes(places: List<Place>): Int {
        val cafes = places.map { createCafeFromPlace(it) }

        val existingIds = cafeRepository.findExistingPlaceIds(cafes.map { it.placeId }).toSet()
        val newCafes = cafes.filter { it.placeId !in existingIds }

        if (newCafes.isEmpty()) return 0

        val savedCafes = cafeRepository.saveAll(newCafes)
        val operatingHours = createOperatingHours(savedCafes, places)
        cafeOperatingHourRepository.saveAll(operatingHours)

        return savedCafes.size
    }

    private fun createCafeFromPlace(place: Place): Cafe {
        val nameText = place.displayName?.text?.trim() ?: "Unknown Cafe"
        val location = place.location ?: throw IllegalArgumentException("Place location is null for ${place.id}")

        return Cafe(
            placeId = place.id,
            name = nameText,
            address = place.formattedAddress?.trim(),
            latitude = BigDecimal.valueOf(location.latitude),
            longitude = BigDecimal.valueOf(location.longitude),
            location = geometryUtils.createPoint(location.longitude, location.latitude)
        )
    }

    private fun createOperatingHours(savedCafes: List<Cafe>, places: List<Place>): List<CafeOperatingHour> {
        val placeMap = places.associateBy { it.id }

        return savedCafes.flatMap { cafe ->
            val periods = placeMap[cafe.placeId]?.regularOpeningHours?.periods.orEmpty()

            periods.mapNotNull { period ->
                val open = period.open
                val close = period.close

                // open 또는 close가 null이거나 hour/minute가 null이면 해당 요일 건너뜀
                if (open == null || close == null ||
                    open.hour == null || open.minute == null ||
                    close.hour == null || close.minute == null) {
                    return@mapNotNull null
                }

                CafeOperatingHour(
                    cafeId = cafe.id!!,
                    dayOfWeek = open.day,
                    openTime = LocalTime.of(open.hour, open.minute),
                    closeTime = LocalTime.of(close.hour, close.minute)
                )
            }
        }
    }
}
