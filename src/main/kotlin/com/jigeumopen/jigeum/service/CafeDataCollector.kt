package com.jigeumopen.jigeum.service

import com.jigeumopen.jigeum.domain.Cafe
import com.jigeumopen.jigeum.external.GooglePlacesClient
import com.jigeumopen.jigeum.repository.CafeRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.LocalTime

@Service
class CafeDataCollector(
    private val googlePlacesClient: GooglePlacesClient,
    private val cafeRepository: CafeRepository
) {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun collectCafesInArea(latitude: Double, longitude: Double, radius: Int = 5000): Int {
        logger.info("===== 카페 데이터 수집 시작: ($latitude, $longitude) =====")

        val result = googlePlacesClient.searchNearbyCafes(latitude, longitude, radius.toDouble())
            .flatMapMany { response -> Flux.fromIterable(response.places ?: emptyList()) }
            .filter { place ->
                val exists = cafeRepository.existsByName(place.displayName?.text ?: "")
                if (exists) {
                    logger.debug("중복 스킵: ${place.displayName?.text}")
                }
                !exists
            }
            .map { place ->
                val openTime = extractOpenTime(place.regularOpeningHours)
                val closeTime = extractCloseTime(place.regularOpeningHours)

                Cafe(
                    name = place.displayName?.text ?: "이름 없음",
                    address = place.formattedAddress,
                    phone = place.nationalPhoneNumber,
                    latitude = BigDecimal.valueOf(place.location?.latitude ?: 0.0),
                    longitude = BigDecimal.valueOf(place.location?.longitude ?: 0.0),
                    location = geometryFactory.createPoint(
                        Coordinate(
                            place.location?.longitude ?: 0.0,
                            place.location?.latitude ?: 0.0
                        )
                    ),
                    openTime = openTime,  // 수정!
                    closeTime = closeTime,
                    category = "카페",
                    rating = place.rating?.let { BigDecimal.valueOf(it) }
                )
            }
            .onErrorContinue { error, _ ->
                logger.error("카페 데이터 변환 실패", error)
            }
            .collectList()
            .subscribeOn(Schedulers.boundedElastic())
            .block() ?: emptyList()

        val savedCafes = cafeRepository.saveAll(result)
        val savedCount = savedCafes.size

        logger.info("===== 수집 완료: $savedCount 개 저장 =====")
        return savedCount
    }

    private fun extractOpenTime(openingHours: com.jigeumopen.jigeum.external.dto.RegularOpeningHours?): LocalTime? {
        val today = java.time.DayOfWeek.from(java.time.LocalDate.now()).value % 7
        val todayPeriod = openingHours?.periods?.find { it.open?.day == today }

        val hour = todayPeriod?.open?.hour ?: return null
        val minute = todayPeriod?.open?.minute ?: 0

        return LocalTime.of(hour, minute)
    }

    private fun extractCloseTime(openingHours: com.jigeumopen.jigeum.external.dto.RegularOpeningHours?): LocalTime {
        val today = java.time.DayOfWeek.from(java.time.LocalDate.now()).value % 7
        val todayPeriod = openingHours?.periods?.find { it.close?.day == today }

        val hour = todayPeriod?.close?.hour ?: 22
        val minute = todayPeriod?.close?.minute ?: 0

        return LocalTime.of(hour, minute)
    }
}
