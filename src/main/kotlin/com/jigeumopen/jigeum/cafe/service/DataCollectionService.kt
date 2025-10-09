package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.client.GooglePlacesClient
import com.jigeumopen.jigeum.cafe.dto.Place
import com.jigeumopen.jigeum.cafe.dto.response.AreaCollectionResponse
import com.jigeumopen.jigeum.cafe.dto.response.BatchCollectionResponse
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.entity.CafeOperatingHour
import com.jigeumopen.jigeum.cafe.repository.CafeOperatingHourRepository
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.config.SeoulGridLocations
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
    private val operatingHourRepository: CafeOperatingHourRepository,
    private val geometryUtils: GeometryUtils,
    private val gridLocations: SeoulGridLocations
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun collectCafesInArea(lat: Double, lng: Double, radius: Int): AreaCollectionResponse {
        logger.info("Starting collection for area: ($lat, $lng), radius: ${radius}m")

        val places = fetchPlacesFromGoogle(lat, lng, radius)
        val savableCount = places.count { it.isSavable() }

        logger.debug("Found {} savable places out of {} total", savableCount, places.size)

        if (savableCount == 0) {
            return AreaCollectionResponse(0, "$lat,$lng", radius)
        }

        val savedCount = saveCafesWithOperatingHours(places.filter { it.isSavable() })

        logger.info("Successfully saved {} new cafes", savedCount)
        return AreaCollectionResponse(savedCount, "$lat,$lng", radius)
    }

    fun collectAllCafes(): BatchCollectionResponse {
        logger.info("Starting batch collection for all Seoul locations")

        val results = ConcurrentHashMap<String, Int>()
        val locations = gridLocations.getAll()

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = locations.map { location ->
                executor.submit {
                    collectWithRetry(location, results)
                }
            }
            futures.forEach { it.get() }
        }

        val totalCount = results.values.sum()
        logger.info("Batch collection completed. Total: {}, Locations: {}", totalCount, results.size)

        return BatchCollectionResponse(
            results = results,
            totalCount = totalCount,
            locations = results.size
        )
    }

    private fun collectWithRetry(
        location: SeoulGridLocations.GridLocation,
        results: ConcurrentHashMap<String, Int>
    ) {
        var attempt = 0
        val maxAttempts = 3

        while (attempt < maxAttempts) {
            try {
                Thread.sleep(2000L * (attempt + 1)) // Exponential backoff

                val response = collectCafesInArea(
                    location.latitude,
                    location.longitude,
                    3000
                )

                results[location.name] = response.savedCount
                logger.debug("Collected {} cafes from {}", response.savedCount, location.name)
                return

            } catch (e: Exception) {
                attempt++
                logger.warn(
                    "Attempt {}/{} failed for {}: {}",
                    attempt, maxAttempts, location.name, e.message
                )

                if (attempt >= maxAttempts) {
                    results[location.name] = 0
                }
            }
        }
    }

    private fun fetchPlacesFromGoogle(lat: Double, lng: Double, radius: Int): List<Place> {
        return try {
            googlePlacesClient.searchNearbyCafes(lat, lng, radius.toDouble())
                .places
                .orEmpty()
        } catch (e: Exception) {
            logger.error("Failed to fetch places from Google API", e)
            emptyList()
        }
    }

    private fun saveCafesWithOperatingHours(places: List<Place>): Int {
        val placeIds = places.map { it.id }
        val existingIds = cafeRepository.findExistingPlaceIds(placeIds)

        val newPlaces = places.filter { it.id !in existingIds }

        if (newPlaces.isEmpty()) {
            logger.debug("No new cafes to save")
            return 0
        }

        val cafes = newPlaces.map { createCafeEntity(it) }
        val savedCafes = cafeRepository.saveAll(cafes)

        val operatingHours = createOperatingHours(savedCafes, newPlaces)
        operatingHourRepository.saveAll(operatingHours)

        return savedCafes.size
    }

    private fun createCafeEntity(place: Place): Cafe {
        val location = place.location
            ?: throw IllegalArgumentException("Location is required for place: ${place.id}")

        return Cafe(
            placeId = place.id,
            name = place.displayName?.text?.trim() ?: "Unknown Cafe",
            address = place.formattedAddress?.trim(),
            latitude = BigDecimal.valueOf(location.latitude),
            longitude = BigDecimal.valueOf(location.longitude),
            location = geometryUtils.createPoint(location.longitude, location.latitude)
        )
    }

    private fun createOperatingHours(
        savedCafes: List<Cafe>,
        places: List<Place>
    ): List<CafeOperatingHour> {
        val placeMap = places.associateBy { it.id }

        return savedCafes.flatMap { cafe ->
            val periods = placeMap[cafe.placeId]
                ?.regularOpeningHours
                ?.periods
                .orEmpty()

            periods.mapNotNull { period ->
                createOperatingHour(cafe.id!!, period.open, period.close)
            }
        }
    }

    private fun createOperatingHour(
        cafeId: Long,
        open: com.jigeumopen.jigeum.cafe.dto.DayTime?,
        close: com.jigeumopen.jigeum.cafe.dto.DayTime?
    ): CafeOperatingHour? {
        if (open?.day == null || open.hour == null || open.minute == null) {
            return null
        }

        if (close?.hour == null || close.minute == null) {
            return null
        }

        return CafeOperatingHour.of(
            cafeId = cafeId,
            dayOfWeek = open.day,
            openTime = LocalTime.of(open.hour, open.minute),
            closeTime = LocalTime.of(close.hour, close.minute)
        )
    }
}
