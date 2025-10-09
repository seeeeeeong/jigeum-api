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
import java.time.DayOfWeek
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

    @Transactional
    fun collectCafesInArea(lat: Double, lng: Double, radius: Int): AreaCollectionResponse {
        val places = googlePlacesClient.searchNearbyCafes(lat, lng, radius.toDouble()).places
        if (places == null || places.isEmpty()) {
            logger.info("No cafes found at ($lat, $lng)")
            return AreaCollectionResponse(0, "$lat,$lng", radius)
        }

        val savedCount = saveNewCafes(places)
        logger.info("Collected $savedCount new cafes at ($lat, $lng)")
        return AreaCollectionResponse(savedCount, "$lat,$lng", radius)
    }

    fun collectAllCafes(): BatchCollectionResponse {
        val results = ConcurrentHashMap<String, Int>()

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = SeoulLocations.GRID_POINTS.map { location ->
                executor.submit {
                    try {
                        Thread.sleep(2000)
                        val count = collectCafesInArea(location.latitude, location.longitude, 3000).savedCount
                        results[location.name] = count
                    } catch (e: Exception) {
                        logger.warn("Failed: ${location.name} - ${e.message}")
                        results[location.name] = 0
                    }
                }
            }
            futures.forEach { it.get() }
        }

        val totalCount = results.values.sum()
        return BatchCollectionResponse(results, totalCount, results.size)
    }

    private fun saveNewCafes(places: List<Place>): Int {
        val cafes = places.mapNotNull { convertToCafe(it) }
        if (cafes.isEmpty()) return 0

        val existingIds = cafeRepository.findExistingPlaceIds(cafes.map { it.placeId }).toSet()
        val newCafes = cafes.filter { it.placeId !in existingIds }

        if (newCafes.isEmpty()) return 0
        return cafeRepository.saveAll(newCafes).size
    }

    private fun convertToCafe(place: Place): Cafe? {
        val displayName = place.displayName?.text?.trim()
        if (displayName.isNullOrEmpty()) return null

        val location = place.location ?: return null
        if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) return null

        val address = place.formattedAddress?.trim()

        return Cafe(
            placeId = place.id,
            name = displayName,
            address = address,
            latitude = BigDecimal.valueOf(location.latitude),
            longitude = BigDecimal.valueOf(location.longitude),
            location = geometryUtils.createPoint(location.longitude, location.latitude),
            openTime = extractTime(place, true),
            closeTime = extractTime(place, false) ?: LocalTime.of(22, 0)
        )
    }

    private fun extractTime(place: Place, isOpen: Boolean): LocalTime? {
        val periods = place.regularOpeningHours?.periods
        if (periods == null) return null

        val today = DayOfWeek.from(java.time.LocalDate.now()).value % 7
        val info = periods.find { period ->
            val target = if (isOpen) period.open else period.close
            target != null && target.day == today
        } ?: return null

        val timeInfo = if (isOpen) info.open else info.close
        if (timeInfo == null) return null

        val hour = timeInfo.hour?.coerceIn(0, 23) ?: if (isOpen) 7 else 22
        val minute = timeInfo.minute?.coerceIn(0, 59) ?: 0
        return LocalTime.of(hour, minute)
    }
}
