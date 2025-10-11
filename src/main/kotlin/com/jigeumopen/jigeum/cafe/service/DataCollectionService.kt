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
    private val googleClient: GooglePlacesClient,
    private val cafeRepository: CafeRepository,
    private val operatingHourRepository: CafeOperatingHourRepository,
    private val geometryUtils: GeometryUtils,
    private val gridLocations: SeoulGridLocations
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun collectCafesInArea(lat: Double, lng: Double, radius: Int): AreaCollectionResponse {
        val places = runCatching {
            googleClient.searchNearbyCafes(lat, lng, radius.toDouble()).places.orEmpty()
        }.getOrElse { emptyList() }

        val saved = saveCafes(places.filter { it.isSavable() })
        log.info("Saved {} cafes at ({}, {})", saved, lat, lng)

        return AreaCollectionResponse(saved, "$lat,$lng", radius)
    }

    fun collectAllCafes(): BatchCollectionResponse {
        val results = ConcurrentHashMap<String, Int>()

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            gridLocations.getAll().map { location ->
                executor.submit {
                    repeat(3) { attempt ->
                        try {
                            Thread.sleep(2000L * (attempt + 1))
                            results[location.name] = collectCafesInArea(
                                location.latitude,
                                location.longitude,
                                3000
                            ).savedCount
                            return@submit
                        } catch (e: Exception) {
                            if (attempt == 2) results[location.name] = 0
                        }
                    }
                }
            }.forEach { it.get() }
        }

        return BatchCollectionResponse(results, results.values.sum(), results.size)
    }

    private fun saveCafes(places: List<Place>): Int {
        if (places.isEmpty()) return 0

        val existingIds = cafeRepository.findExistingPlaceIds(places.map { it.id })
        val newPlaces = places.filter { it.id !in existingIds }

        if (newPlaces.isEmpty()) return 0

        val cafes = newPlaces.mapNotNull { place ->
            runCatching {
                Cafe(
                    placeId = place.id,
                    name = place.displayName?.text?.trim() ?: "Unknown",
                    address = place.formattedAddress?.trim(),
                    latitude = BigDecimal.valueOf(place.location!!.latitude),
                    longitude = BigDecimal.valueOf(place.location.longitude),
                    location = geometryUtils.createPoint(place.location.longitude, place.location.latitude)
                )
            }.getOrNull()
        }

        val saved = cafeRepository.saveAll(cafes)
        val hours = saved.flatMap { cafe ->
            val place = newPlaces.first { it.id == cafe.placeId }
            place.regularOpeningHours?.periods.orEmpty().mapNotNull { period ->
                val open = period.open ?: return@mapNotNull null
                val close = period.close ?: return@mapNotNull null

                if (open.day == null || open.hour == null || open.minute == null ||
                    close.hour == null || close.minute == null) return@mapNotNull null

                CafeOperatingHour.of(
                    cafeId = cafe.id!!,
                    dayOfWeek = open.day,
                    openTime = LocalTime.of(open.hour, open.minute),
                    closeTime = LocalTime.of(close.hour, close.minute)
                )
            }
        }

        operatingHourRepository.saveAll(hours)
        return saved.size
    }
}
