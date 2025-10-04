package com.jigeumopen.jigeum.service

import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.common.constants.CafeConstants.GooglePlaces.COLLECTION_DELAY_MS
import com.jigeumopen.jigeum.common.constants.CafeConstants.SeoulLocations
import com.jigeumopen.jigeum.common.util.GeometryUtils
import com.jigeumopen.jigeum.domain.entity.Cafe
import com.jigeumopen.jigeum.domain.repository.CafeRepository
import com.jigeumopen.jigeum.external.dto.Place
import com.jigeumopen.jigeum.external.dto.RegularOpeningHours
import com.jigeumopen.jigeum.infrastructure.client.GooglePlacesClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicInteger

@Service
class DataCollectionService(
    private val googlePlacesClient: GooglePlacesClient,
    private val cafeRepository: CafeRepository,
    private val geometryUtils: GeometryUtils
) {
    private val collectionInProgress = AtomicInteger(0)
    private val totalCollected = AtomicInteger(0)

    @Transactional
    fun collectCafesInArea(latitude: Double, longitude: Double, radius: Int): Int {
        collectionInProgress.incrementAndGet()

        return try {
            val places = fetchCafesFromGoogle(latitude, longitude, radius)
            val savedCount = saveCafes(places)
            totalCollected.addAndGet(savedCount)
            savedCount
        } catch (e: Exception) {
            throw BusinessException(ErrorCode.GOOGLE_API_ERROR)
        } finally {
            collectionInProgress.decrementAndGet()
        }
    }

    fun collectAllSeoulCafes(): Map<String, Int> {

        val results = mutableMapOf<String, Int>()

        SeoulLocations.GRID_POINTS.forEach { location ->
            try {
                Thread.sleep(COLLECTION_DELAY_MS)
                val count = collectCafesInArea(
                    location.latitude,
                    location.longitude,
                    3000
                )
                results[location.name] = count
            } catch (e: Exception) {
                results[location.name] = 0
            }
        }

        return results
    }

    fun getCollectionStatus(): Map<String, Any> {
        val totalCafes = cafeRepository.count()
        val openNow = cafeRepository.countOpenCafesAt(LocalTime.now())

        return mapOf(
            "inProgress" to collectionInProgress.get(),
            "totalCollected" to totalCollected.get(),
            "totalCafes" to totalCafes,
            "openNow" to openNow,
            "lastUpdate" to LocalTime.now()
        )
    }

    private fun fetchCafesFromGoogle(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): List<Place> {
        return googlePlacesClient.searchNearbyCafes(
            latitude,
            longitude,
            radius.toDouble()
        )
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
            .filter { place ->
                val name = place.displayName?.text ?: ""
                name.isNotBlank() && !cafeRepository.existsByName(name)
            }
            .mapNotNull { place -> convertToCafe(place) }

        return if (newCafes.isNotEmpty()) {
            val saved = cafeRepository.saveAll(newCafes)
            saved.size
        } else {
            0
        }
    }

    private fun convertToCafe(place: Place): Cafe? {
        return try {
            val name = place.displayName?.text ?: return null
            val lat = place.location?.latitude ?: return null
            val lng = place.location?.longitude ?: return null

            val openTime = extractOpenTime(place.regularOpeningHours)
            val closeTime = extractCloseTime(place.regularOpeningHours)
                ?: LocalTime.of(22, 0)

            Cafe(
                name = name,
                address = place.formattedAddress,
                phone = formatPhoneNumber(place.nationalPhoneNumber),
                latitude = BigDecimal.valueOf(lat),
                longitude = BigDecimal.valueOf(lng),
                location = geometryUtils.createPoint(lng, lat),
                openTime = openTime,
                closeTime = closeTime,
                category = extractCategory(place.types),
                rating = place.rating?.let { BigDecimal.valueOf(it) }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun isValidPlace(place: Place): Boolean {
        return !place.displayName?.text.isNullOrBlank() &&
                place.location?.latitude != null &&
                place.location.longitude != null
    }

    private fun extractOpenTime(openingHours: RegularOpeningHours?): LocalTime? {
        val todayIndex = getTodayIndex()
        return openingHours?.periods
            ?.find { it.open?.day == todayIndex }
            ?.open
            ?.let { dayTime ->
                LocalTime.of(dayTime.hour ?: 7, dayTime.minute ?: 0)
            }
    }

    private fun extractCloseTime(openingHours: RegularOpeningHours?): LocalTime? {
        val todayIndex = getTodayIndex()
        return openingHours?.periods
            ?.find { period ->
                period.close?.day == todayIndex ||
                        (period.open?.day == todayIndex && period.close == null)
            }
            ?.close
            ?.let { dayTime ->
                LocalTime.of(dayTime.hour ?: 22, dayTime.minute ?: 0)
            }
    }

    private fun getTodayIndex(): Int {
        return when (LocalDate.now().dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }
    }

    private fun formatPhoneNumber(phone: String?): String? {
        return phone?.replace(Regex("\\s+"), "-")
            ?.replace(Regex("^\\+82"), "0")
    }

    private fun extractCategory(types: List<String>?): String {
        val categoryMap = mapOf(
            "cafe" to "카페",
            "coffee_shop" to "커피전문점",
            "restaurant" to "카페&레스토랑",
            "bakery" to "베이커리카페",
            "dessert" to "디저트카페"
        )

        types?.forEach { type ->
            categoryMap[type]?.let { return it }
        }

        return "카페"
    }
}
