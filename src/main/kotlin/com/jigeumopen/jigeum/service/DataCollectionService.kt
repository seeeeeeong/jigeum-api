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
            val savedCount = saveNewCafes(places)
            totalCollected.addAndGet(savedCount)
            savedCount
        } catch (ex: Exception) {
            throw BusinessException(ErrorCode.GOOGLE_API_ERROR, ex)
        } finally {
            collectionInProgress.decrementAndGet()
        }
    }

    fun collectAllSeoulCafes(): Map<String, Int> =
        SeoulLocations.GRID_POINTS.associate { location ->
            Thread.sleep(COLLECTION_DELAY_MS)
            val count = runCatching {
                collectCafesInArea(location.latitude, location.longitude, 3000)
            }.getOrDefault(0)
            location.name to count
        }

    fun getCollectionStatus(): Map<String, Any> = mapOf(
        "inProgress" to collectionInProgress.get(),
        "totalCollected" to totalCollected.get(),
        "totalCafes" to cafeRepository.count(),
        "openNow" to cafeRepository.countByCloseTimeAfter(LocalTime.now()),
        "lastUpdate" to LocalTime.now()
    )

    private fun fetchCafesFromGoogle(lat: Double, lng: Double, radius: Int): List<Place> =
        googlePlacesClient.searchNearbyCafes(lat, lng, radius.toDouble())
            .flatMapMany { Flux.fromIterable(it.places.orEmpty()) }
            .filter { it.isValid() }
            .collectList()
            .subscribeOn(Schedulers.boundedElastic())
            .block().orEmpty()

    private fun saveNewCafes(places: List<Place>): Int =
        places.asSequence()
            .mapNotNull { convertToCafe(it) }
            .filterNot { cafeRepository.existsByName(it.name) }
            .toList()
            .let { newCafes ->
                if (newCafes.isEmpty()) 0
                else cafeRepository.saveAll(newCafes).size
            }

    private fun convertToCafe(place: Place): Cafe? = runCatching {
        val name = place.displayName?.text ?: return null
        val lat = place.location?.latitude ?: return null
        val lng = place.location?.longitude ?: return null

        Cafe(
            name = name,
            address = place.formattedAddress,
            phone = formatPhoneNumber(place.nationalPhoneNumber),
            latitude = BigDecimal.valueOf(lat),
            longitude = BigDecimal.valueOf(lng),
            location = geometryUtils.createPoint(lng, lat),
            openTime = extractOpenTime(place.regularOpeningHours),
            closeTime = extractCloseTime(place.regularOpeningHours) ?: LocalTime.of(22, 0),
            category = extractCategory(place.types),
            rating = place.rating?.let { BigDecimal.valueOf(it) }
        )
    }.getOrNull()

    private fun Place.isValid(): Boolean =
        !displayName?.text.isNullOrBlank() &&
                location?.latitude != null &&
                location?.longitude != null

    private fun extractOpenTime(hours: RegularOpeningHours?): LocalTime? =
        hours?.periods
            ?.find { it.open?.day == todayIndex() }
            ?.open
            ?.let { LocalTime.of(it.hour ?: 7, it.minute ?: 0) }

    private fun extractCloseTime(hours: RegularOpeningHours?): LocalTime? =
        hours?.periods
            ?.find { it.close?.day == todayIndex() || (it.open?.day == todayIndex() && it.close == null) }
            ?.close
            ?.let { LocalTime.of(it.hour ?: 22, it.minute ?: 0) }

    private fun todayIndex(): Int = LocalDate.now().dayOfWeek.value % 7

    private fun formatPhoneNumber(phone: String?): String? =
        phone?.replace(Regex("\\s+"), "-")?.replace(Regex("^\\+82"), "0")

    private fun extractCategory(types: List<String>?): String {
        val categoryMap = mapOf(
            "cafe" to "카페",
            "coffee_shop" to "커피전문점",
            "restaurant" to "카페&레스토랑",
            "bakery" to "베이커리카페",
            "dessert" to "디저트카페"
        )
        return types?.firstNotNullOfOrNull { categoryMap[it] } ?: "카페"
    }
}
