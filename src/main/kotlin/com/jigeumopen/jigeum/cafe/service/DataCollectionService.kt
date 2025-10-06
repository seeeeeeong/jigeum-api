package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.client.GooglePlacesClient
import com.jigeumopen.jigeum.cafe.dto.Place
import com.jigeumopen.jigeum.cafe.dto.response.AreaCollectionResponse
import com.jigeumopen.jigeum.cafe.dto.response.BatchCollectionResponse
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.constants.CafeConstants.SeoulLocations
import com.jigeumopen.jigeum.common.util.GeometryUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Service
class DataCollectionService(
    private val googlePlacesClient: GooglePlacesClient,
    private val cafeRepository: CafeRepository,
    private val geometryUtils: GeometryUtils
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val COLLECTION_RADIUS = 3000
        private const val DELAY_MS = 2000L
        private const val DEFAULT_OPEN_HOUR = 7
        private const val DEFAULT_CLOSE_HOUR = 22
    }

    @Transactional
    fun collectCafesInArea(lat: Double, lng: Double, radius: Int): AreaCollectionResponse {
        val places = googlePlacesClient.searchNearbyCafes(lat, lng, radius.toDouble())
            .places
            ?.filter { it.isValid() }
            ?: emptyList()

        val savedCount = saveCafes(places)
        logger.info("Collected $savedCount cafes at ($lat, $lng)")

        return AreaCollectionResponse(savedCount, "$lat,$lng", radius)
    }

    fun collectAllCafes(): BatchCollectionResponse {
        val results = ConcurrentHashMap<String, Int>()

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            SeoulLocations.GRID_POINTS.map { location ->
                executor.submit {
                    try {
                        Thread.sleep(DELAY_MS)
                        val response = collectCafesInArea(
                            location.latitude,
                            location.longitude,
                            COLLECTION_RADIUS
                        )
                        results[location.name] = response.savedCount
                    } catch (e: Exception) {
                        logger.warn("Failed to collect ${location.name}: ${e.message}")
                        results[location.name] = 0
                    }
                }
            }.forEach { it.get() }
        }

        return BatchCollectionResponse(
            results = results,
            totalCount = results.values.sum(),
            locations = results.size
        )
    }

    private fun saveCafes(places: List<Place>): Int {
        val cafes = places.mapNotNull { it.toCafe() }
        if (cafes.isEmpty()) return 0

        val existingIds = cafeRepository.findExistingPlaceIds(cafes.map { it.placeId }).toSet()
        val newCafes = cafes.filterNot { it.placeId in existingIds }

        return if (newCafes.isNotEmpty()) {
            cafeRepository.saveAll(newCafes).size
        } else {
            0
        }
    }

    private fun Place.isValid(): Boolean =
        !displayName?.text.isNullOrBlank() &&
                location?.latitude in -90.0..90.0 &&
                location?.longitude in -180.0..180.0

    private fun Place.toCafe(): Cafe? = runCatching {
        val name = displayName?.text?.trim() ?: return null
        val lat = location?.latitude ?: return null
        val lng = location?.longitude ?: return null

        Cafe(
            placeId = id,
            name = name,
            address = formattedAddress?.trim(),
            latitude = BigDecimal.valueOf(lat),
            longitude = BigDecimal.valueOf(lng),
            location = geometryUtils.createPoint(lng, lat),
            openTime = extractOpenTime(),
            closeTime = extractCloseTime()
        )
    }.getOrNull()

    private fun Place.extractOpenTime(): LocalTime? {
        val day = LocalDate.now().dayOfWeek.value % 7
        return regularOpeningHours?.periods
            ?.find { it.open?.day == day }
            ?.open?.let {
                LocalTime.of(
                    it.hour?.coerceIn(0, 23) ?: DEFAULT_OPEN_HOUR,
                    it.minute?.coerceIn(0, 59) ?: 0
                )
            }
    }

    private fun Place.extractCloseTime(): LocalTime {
        val day = LocalDate.now().dayOfWeek.value % 7
        val closeInfo = regularOpeningHours?.periods
            ?.find { it.close?.day == day }
            ?.close

        return closeInfo?.let {
            LocalTime.of(
                it.hour?.coerceIn(0, 23) ?: DEFAULT_CLOSE_HOUR,
                it.minute?.coerceIn(0, 59) ?: 0
            )
        } ?: LocalTime.of(DEFAULT_CLOSE_HOUR, 0)
    }
}
