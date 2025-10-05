package com.jigeumopen.jigeum.cafe.service

import com.jigeumopen.jigeum.cafe.client.GooglePlacesClient
import com.jigeumopen.jigeum.cafe.dto.Place
import com.jigeumopen.jigeum.cafe.dto.RegularOpeningHours
import com.jigeumopen.jigeum.cafe.dto.response.AreaCollectionResponse
import com.jigeumopen.jigeum.cafe.dto.response.BatchCollectionResponse
import com.jigeumopen.jigeum.cafe.dto.response.CollectionStatusResponse
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.constants.CafeConstants.GooglePlaces.COLLECTION_DELAY_MS
import com.jigeumopen.jigeum.common.constants.CafeConstants.SeoulLocations
import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import com.jigeumopen.jigeum.common.util.GeometryUtils
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
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

    companion object {
        private const val SEOUL_COLLECTION_RADIUS = 3000
        private const val BATCH_SIZE = 100
    }


    @Transactional
    fun collectCafesInAreaReactive(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Mono<AreaCollectionResponse> {
        collectionInProgress.incrementAndGet()

        return fetchCafesFromGoogleApi(latitude, longitude, radius)
            .flatMap { places -> saveNewCafesReactive(places) }
            .doOnNext { savedCount ->
                totalCollected.addAndGet(savedCount)
                logger.info("Collected $savedCount new cafes at ($latitude, $longitude) with radius ${radius}m")
            }
            .map { savedCount ->
                AreaCollectionResponse(
                    savedCount = savedCount,
                    location = "$latitude,$longitude",
                    radius = radius
                )
            }
            .doOnError { ex ->
                logger.error("Failed to collect cafes at ($latitude, $longitude): ${ex.message}", ex)
            }
            .doFinally { collectionInProgress.decrementAndGet() }
            .onErrorMap { ex ->
                when (ex) {
                    is BusinessException -> ex
                    else -> BusinessException(ErrorCode.SEARCH_ERROR, ex)
                }
            }
    }


    @Transactional
    fun collectCafesInArea(
        latitude: Double,
        longitude: Double,
        radius: Int
    ): AreaCollectionResponse {
        return collectCafesInAreaReactive(latitude, longitude, radius)
            .block() ?: throw BusinessException(ErrorCode.SEARCH_ERROR)
    }

    fun collectAllCafes(): BatchCollectionResponse {
        val locationCount = SeoulLocations.GRID_POINTS.size
        logger.info("Starting Seoul-wide cafe collection for $locationCount locations")

        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Int>()

        try {
            Flux.fromIterable(SeoulLocations.GRID_POINTS)
                .delayElements(Duration.ofMillis(COLLECTION_DELAY_MS))
                .flatMap { location ->
                    collectCafesInAreaReactive(
                        location.latitude,
                        location.longitude,
                        SEOUL_COLLECTION_RADIUS
                    )
                        .map { response -> location.name to response.savedCount }
                        .onErrorResume { ex ->
                            logger.warn("Failed to collect cafes in ${location.name}: ${ex.message}")
                            Mono.just(location.name to 0)
                        }
                }
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext { (name, count) -> results[name] = count }
                .blockLast()

            val duration = System.currentTimeMillis() - startTime
            val totalCount = results.values.sum()

            logger.info("Completed Seoul-wide collection: $totalCount cafes in ${results.size} locations (${duration}ms)")

            return BatchCollectionResponse(
                results = results,
                totalCount = totalCount,
                locations = results.size
            )
        } catch (ex: Exception) {
            logger.error("Seoul-wide collection failed: ${ex.message}", ex)
            throw BusinessException(ErrorCode.SEARCH_ERROR, ex)
        }
    }

    fun getCollectionStatus(): CollectionStatusResponse {
        return try {
            CollectionStatusResponse(
                inProgress = collectionInProgress.get(),
                totalCollected = totalCollected.get(),
                totalCafes = cafeRepository.count(),
                openNow = cafeRepository.countByCloseTimeAfter(LocalTime.now()),
                lastUpdate = LocalTime.now().toString()
            )
        } catch (ex: DataAccessException) {
            logger.error("Failed to get collection status: ${ex.message}", ex)
            throw BusinessException(ErrorCode.DATABASE_ERROR, ex)
        }
    }

    private fun fetchCafesFromGoogleApi(
        lat: Double,
        lng: Double,
        radius: Int
    ): Mono<List<Place>> {
        return googlePlacesClient.searchNearbyCafesReactive(lat, lng, radius.toDouble())
            .flatMapMany { response -> Flux.fromIterable(response.places.orEmpty()) }
            .filter { it.hasRequiredFields() }
            .collectList()
            .subscribeOn(Schedulers.boundedElastic())
    }

    private fun saveNewCafesReactive(places: List<Place>): Mono<Int> {
        return Mono.fromCallable {
            val cafes = places.mapNotNull { place -> convertToCafe(place) }

            if (cafes.isEmpty()) {
                logger.debug("No valid cafes to save from ${places.size} places")
                return@fromCallable 0
            }

            try {
                val existingNames = cafeRepository.findNamesByNameIn(cafes.map { it.name }).toSet()
                val newCafes = cafes.filterNot { cafe -> existingNames.contains(cafe.name) }

                if (newCafes.isEmpty()) {
                    logger.debug("All ${cafes.size} cafes already exist in database")
                    return@fromCallable 0
                }

                val savedCafes = cafeRepository.saveAll(newCafes)
                logger.debug("Saved ${savedCafes.size} new cafes out of ${cafes.size} total")
                savedCafes.size
            } catch (ex: DataAccessException) {
                logger.error("Database error while saving cafes: ${ex.message}", ex)
                throw BusinessException(ErrorCode.SAVE_ERROR, ex)
            } catch (ex: Exception) {
                logger.error("Unexpected error while saving cafes: ${ex.message}", ex)
                throw BusinessException(ErrorCode.SAVE_ERROR, ex)
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }

    private fun convertToCafe(place: Place): Cafe? = runCatching {
        val name = place.displayName?.text?.trim()?.takeIf { it.isNotBlank() }
            ?: return null
        val latitude = place.location?.latitude
            ?: return null
        val longitude = place.location?.longitude
            ?: return null

        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            logger.warn("Invalid coordinates for cafe '$name': lat=$latitude, lng=$longitude")
            return null
        }

        Cafe(
            name = name,
            address = place.formattedAddress?.trim()?.takeIf { it.isNotBlank() },
            phone = PhoneNumberFormatter.format(place.nationalPhoneNumber),
            latitude = BigDecimal.valueOf(latitude),
            longitude = BigDecimal.valueOf(longitude),
            location = geometryUtils.createPoint(longitude, latitude),
            openTime = OpeningHoursExtractor.extractOpenTime(place.regularOpeningHours),
            closeTime = OpeningHoursExtractor.extractCloseTime(place.regularOpeningHours),
            category = CategoryExtractor.extractCategory(place.types),
            rating = place.rating?.takeIf { it in 0.0..5.0 }?.let { BigDecimal.valueOf(it) }
        )
    }.onFailure { ex ->
        logger.debug("Failed to convert place '${place.displayName?.text}' to cafe: ${ex.message}")
    }.getOrNull()

    private fun Place.hasRequiredFields(): Boolean =
        !displayName?.text.isNullOrBlank() &&
                location?.latitude != null &&
                location?.longitude != null &&
                location.latitude in -90.0..90.0 &&
                location.longitude in -180.0..180.0
}


private object OpeningHoursExtractor {
    private const val DEFAULT_OPEN_HOUR = 7
    private const val DEFAULT_CLOSE_HOUR = 22
    private const val DEFAULT_MINUTE = 0

    fun extractOpenTime(hours: RegularOpeningHours?): LocalTime? {
        val currentDay = getCurrentDayIndex()

        return hours?.periods
            ?.find { it.open?.day == currentDay }
            ?.open
            ?.let { openInfo ->
                try {
                    LocalTime.of(
                        openInfo.hour?.coerceIn(0, 23) ?: DEFAULT_OPEN_HOUR,
                        openInfo.minute?.coerceIn(0, 59) ?: DEFAULT_MINUTE
                    )
                } catch (ex: Exception) {
                    LoggerFactory.getLogger(javaClass)
                        .warn("Invalid open time: hour=${openInfo.hour}, minute=${openInfo.minute}")
                    null
                }
            }
    }

    fun extractCloseTime(hours: RegularOpeningHours?): LocalTime {
        val currentDay = getCurrentDayIndex()

        val closeInfo = hours?.periods
            ?.find { period ->
                period.close?.day == currentDay ||
                        (period.open?.day == currentDay && period.close == null)
            }
            ?.close

        return closeInfo?.let {
            try {
                LocalTime.of(
                    it.hour?.coerceIn(0, 23) ?: DEFAULT_CLOSE_HOUR,
                    it.minute?.coerceIn(0, 59) ?: DEFAULT_MINUTE
                )
            } catch (ex: Exception) {
                LoggerFactory.getLogger(javaClass)
                    .warn("Invalid close time: hour=${it.hour}, minute=${it.minute}")
                LocalTime.of(DEFAULT_CLOSE_HOUR, DEFAULT_MINUTE)
            }
        } ?: LocalTime.of(DEFAULT_CLOSE_HOUR, DEFAULT_MINUTE)
    }

    private fun getCurrentDayIndex(): Int = LocalDate.now().dayOfWeek.value % 7
}

private object PhoneNumberFormatter {
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val KOREAN_COUNTRY_CODE_REGEX = Regex("^\\+82")
    private val INVALID_CHARS_REGEX = Regex("[^0-9-]")

    fun format(phoneNumber: String?): String? {
        return phoneNumber
            ?.trim()
            ?.replace(WHITESPACE_REGEX, "-")
            ?.replace(KOREAN_COUNTRY_CODE_REGEX, "0")
            ?.replace(INVALID_CHARS_REGEX, "")
            ?.takeIf { it.length >= 9 && it.isNotBlank() } // 최소 9자리 이상
    }
}

private object CategoryExtractor {
    private val CATEGORY_MAP = mapOf(
        "cafe" to "카페",
        "coffee_shop" to "커피전문점",
        "restaurant" to "카페&레스토랑",
        "bakery" to "베이커리카페",
        "dessert" to "디저트카페",
        "tea_house" to "티하우스"
    )
    private const val DEFAULT_CATEGORY = "카페"

    fun extractCategory(types: List<String>?): String {
        return types
            ?.asSequence()
            ?.mapNotNull { type -> CATEGORY_MAP[type.lowercase()] }
            ?.firstOrNull()
            ?: DEFAULT_CATEGORY
    }
}
