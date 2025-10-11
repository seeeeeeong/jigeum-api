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
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalTime

@Service
class DataCollectionService(
    private val googleClient: GooglePlacesClient,
    private val cafeRepository: CafeRepository,
    private val operatingHourRepository: CafeOperatingHourRepository,
    private val geometryUtils: GeometryUtils,
    private val gridLocations: SeoulGridLocations
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun collectCafesInArea(
        lat: Double,
        lng: Double,
        radius: Int
    ): AreaCollectionResponse = coroutineScope {
        val places = runCatching {
            googleClient.searchNearbyCafes(lat, lng, radius.toDouble()).places.orEmpty()
        }.getOrElse {
            logger.error("Failed to fetch cafes at ($lat, $lng)", it)
            emptyList()
        }

        val saved = withContext(Dispatchers.IO) {
            saveCafes(places.filter { it.isSavable() })
        }

        logger.info("Saved {} cafes at ({}, {})", saved, lat, lng)

        AreaCollectionResponse(
            savedCount = saved,
            location = "$lat,$lng",
            radius = radius
        )
    }

    suspend fun collectAllCafes(): BatchCollectionResponse = coroutineScope {
        val results = gridLocations.getAll()
            .map { location ->
                async {
                    location.name to collectWithRetry(
                        lat = location.latitude,
                        lng = location.longitude,
                        radius = 3000
                    )
                }
            }
            .awaitAll()
            .toMap()

        BatchCollectionResponse(
            results = results,
            totalCount = results.values.sum(),
            locations = results.size
        )
    }

    private suspend fun collectWithRetry(
        lat: Double,
        lng: Double,
        radius: Int,
        maxRetries: Int = 3
    ): Int = coroutineScope {
        repeat(maxRetries) { attempt ->
            try {
                delay(2000L * (attempt + 1))
                return@coroutineScope collectCafesInArea(lat, lng, radius).savedCount
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    logger.error("Failed to collect after $maxRetries attempts at ($lat, $lng)", e)
                    return@coroutineScope 0
                }
            }
        }
        0
    }

    @Transactional
    private fun saveCafes(places: List<Place>): Int {
        if (places.isEmpty()) return 0

        val placeIds = places.map { it.id }
        val existingIds = cafeRepository.findExistingPlaceIds(placeIds)
        val newPlaces = places.filterNot { it.id in existingIds }

        if (newPlaces.isEmpty()) return 0

        val cafes = newPlaces.mapNotNull { place ->
            place.toCafeEntity(geometryUtils)
        }

        val savedCafes = cafeRepository.saveAll(cafes)
        val operatingHours = savedCafes.flatMap { cafe ->
            val place = newPlaces.first { it.id == cafe.placeId }
            place.toOperatingHours(cafe.id!!)
        }

        operatingHourRepository.saveAll(operatingHours)
        return savedCafes.size
    }
}

private fun Place.toCafeEntity(geometryUtils: GeometryUtils): Cafe? {
    val location = location ?: return null
    val name = displayName?.text?.trim() ?: return null

    return runCatching {
        Cafe(
            placeId = id,
            name = name,
            address = formattedAddress?.trim(),
            latitude = BigDecimal.valueOf(location.latitude),
            longitude = BigDecimal.valueOf(location.longitude),
            location = geometryUtils.createPoint(location.longitude, location.latitude)
        )
    }.getOrNull()
}

private fun Place.toOperatingHours(cafeId: Long): List<CafeOperatingHour> {
    return regularOpeningHours?.periods.orEmpty().mapNotNull { period ->
        val open = period.open ?: return@mapNotNull null
        val close = period.close ?: return@mapNotNull null

        if (open.day == null || open.hour == null || close.hour == null) {
            return@mapNotNull null
        }

        CafeOperatingHour(
            cafeId = cafeId,
            dayOfWeek = open.day,
            openTime = LocalTime.of(open.hour, open.minute ?: 0),
            closeTime = LocalTime.of(close.hour, close.minute ?: 0)
        )
    }
}
