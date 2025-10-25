package com.jigeumopen.jigeum.cafe.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.entity.CafeOperatingHour
import com.jigeumopen.jigeum.cafe.repository.CafeOperatingHourRepository
import com.jigeumopen.jigeum.infrastructure.client.dto.RegularOpeningHours
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class CafeOperatingHourService(
    private val operatingHourRepository: CafeOperatingHourRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun processOperatingHour(cafe: Cafe, openingHoursJson: String) {
        try {
            operatingHourRepository.deleteByPlaceId(cafe.placeId)

            val openingHours = objectMapper.readValue<RegularOpeningHours>(openingHoursJson)
            val operatingHours = openingHours.periods
                ?.mapNotNull { period ->
                    period.close?.let {
                        CafeOperatingHour.of(
                            placeId = cafe.placeId,
                            dayOfWeek = period.open.day,
                            openTime = LocalTime.of(period.open.hour, period.open.minute),
                            closeTime = LocalTime.of(it.hour, it.minute)
                        )
                    }
                }
                .orEmpty()

            operatingHourRepository.saveAll(operatingHours)

        } catch (e: Exception) {
            logger.error("Failed to process operating hours for cafe: {} - {}", cafe.placeId, e.message)
        }
    }

}
