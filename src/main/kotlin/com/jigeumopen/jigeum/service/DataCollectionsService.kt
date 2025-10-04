package com.jigeumopen.jigeum.service

import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.common.constants.CafeConstants.GooglePlaces.COLLECTION_DELAY_MS
import com.jigeumopen.jigeum.common.constants.CafeConstants.SeoulLocations
import com.jigeumopen.jigeum.common.util.GeometryUtils
import com.jigeumopen.jigeum.domain.entity.Cafe
import com.jigeumopen.jigeum.domain.repository.CafeRepository
import com.jigeumopen.jigeum.infrastructure.client.GooglePlacesClient
import com.jigeumopen.jigeum.infrastructure.dto.Place
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicInteger

@Service
class DataCollectionService(
    private val googlePlacesClient: GooglePlacesClient,
    private val cafeRepository: CafeRepository,
    private val geometryUtils: GeometryUtils
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val collectionInProgress = AtomicInteger(0)
    private val totalCollected = AtomicInteger(0)

    @Transactional
    fun collectCafesInArea(latitude: Double, longitude: Double, radius: Int): Int {
        logger.info("===== 카페 데이터 수집 시작: ($latitude, $longitude) radius=${radius}m =====")
        collectionInProgress.incrementAndGet()

        return try {
            val places = fetchCafesFromGoogle(latitude, longitude, radius)
            val savedCount = saveCafes(places)
            totalCollected.addAndGet(savedCount)

            logger.info("===== 수집 완료: $savedCount 개 저장 =====")
            savedCount
        } catch (e: Exception) {
            logger.error("데이터 수집 실패", e)
            throw BusinessException.externalApi("Google Places API 호출 실패: ${e.message}", e)
        } finally {
            collectionInProgress.decrementAndGet()
        }
    }

    fun collectAllSeoulCafes(): Map<String, Int> {
        logger.info("===== 서울 전역 카페 데이터 수집 시작 =====")

        val results = mutableMapOf<String, Int>()

        SeoulLocations.GRID_POINTS.forEach { location ->
            logger.info("[${location.name}] 데이터 수집 시작...")

            try {
                Thread.sleep(COLLECTION_DELAY_MS)  // Rate limit 대응
                val count = collectCafesInArea(location.latitude, location.longitude, 3000)
                results[location.name] = count
                logger.info("[${location.name}] 완료: $count 개 저장")
            } catch (e: Exception) {
                logger.error("[${location.name}] 수집 실패", e)
                results[location.name] = 0
            }
        }

        val totalSaved = results.values.sum()
        logger.info("===== 전체 수집 완료: 총 $totalSaved 개 저장 =====")

        return results
    }

    fun getCollectionStatus(): Map<String, Any> {
        return mapOf(
            "inProgress" to collectionInProgress.get(),
            "totalCollected" to totalCollected.get(),
            "lastUpdate" to LocalTime.now()
        )
    }

    private fun fetchCafesFromGoogle(latitude: Double, longitude: Double, radius: Int): List<Place> {
        return googlePlacesClient.searchNearbyCafes(latitude, longitude, radius.toDouble())
            .flatMapMany { response ->
                Flux.fromIterable(response.places ?: emptyList())
            }
            .filter { place -> isValidPlace(place) }
            .collectList()
            .subscribeOn(Schedulers.boundedElastic())
            .block() ?: emptyList()
    }

    private fun saveCafes(places: List<Place>): Int {
        val newCafes = places
            .filter { place -> !cafeRepository.existsByName(place.displayName?.text ?: "") }
            .mapNotNull { place -> convertToCafe(place) }

        return if (newCafes.isNotEmpty()) {
            cafeRepository.saveAll(newCafes).size
        } else {
            0
        }
    }

    private fun convertToCafe(place: Place): Cafe? {
        return try {
            val name = place.displayName?.text ?: return null
            val lat = place.location?.latitude ?: return null
            val lng = place.location?.longitude ?: return null

            Cafe(
                name = name,
                address = place.formattedAddress,
                phone = place.nationalPhoneNumber,
                latitude = BigDecimal.valueOf(lat),
                longitude = BigDecimal.valueOf(lng),
                location = geometryUtils.createPoint(lng, lat),
                openTime = extractOpenTime(place.regularOpeningHours),
                closeTime = extractCloseTime(place.regularOpeningHours) ?: LocalTime.of(22, 0),
                category = "카페",
                rating = place.rating?.let { BigDecimal.valueOf(it) }
            )
        } catch (e: Exception) {
            logger.warn("카페 변환 실패: ${place.displayName?.text}", e)
            null
        }
    }

    private fun isValidPlace(place: Place): Boolean {
        return !place.displayName?.text.isNullOrBlank() &&
                place.location?.latitude != null &&
                place.location?.longitude != null
    }

    private fun extractOpenTime(openingHours: com.jigeumopen.jigeum.infrastructure.dto.RegularOpeningHours?): LocalTime? {
        val today = java.time.DayOfWeek.from(java.time.LocalDate.now()).value % 7
        return openingHours?.periods
            ?.find { it.open?.day == today }
            ?.open
            ?.let { LocalTime.of(it.hour ?: 7, it.minute ?: 0) }
    }

    private fun extractCloseTime(openingHours: com.jigeumopen.jigeum.infrastructure.dto.RegularOpeningHours?): LocalTime? {
        val today = java.time.DayOfWeek.from(java.time.LocalDate.now()).value % 7
        return openingHours?.periods
            ?.find { it.close?.day == today }
            ?.close
            ?.let { LocalTime.of(it.hour ?: 22, it.minute ?: 0) }
    }
}
