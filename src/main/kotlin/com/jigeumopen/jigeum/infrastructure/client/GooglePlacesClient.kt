package com.jigeumopen.jigeum.infrastructure.client

import com.jigeumopen.jigeum.common.config.GooglePlacesConfig
import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import com.jigeumopen.jigeum.infrastructure.client.dto.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class GooglePlacesClient(
    private val config: GooglePlacesConfig,
    private val retryTemplate: RetryTemplate,
    private val meterRegistry: MeterRegistry,
    webClientBuilder: WebClient.Builder
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val webClient = webClientBuilder
        .baseUrl(config.baseUrl)
        .defaultHeader("X-Goog-Api-Key", config.apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build()

    private val apiCallCounter: Counter = meterRegistry.counter("google.places.api.calls")
    private val apiErrorCounter: Counter = meterRegistry.counter("google.places.api.errors")
    private val apiTimer: Timer = meterRegistry.timer("google.places.api.duration")

    companion object {
        private val FIELD_MASK = listOf(
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.regularOpeningHours"
        ).joinToString(",")
    }

    suspend fun searchNearbyCafes(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): SearchNearbyResponse {
        val request = SearchNearbyRequest(
            includedTypes = listOf("cafe"),
            maxResultCount = 20,
            locationRestriction = LocationRestriction(
                circle = Circle(
                    center = LatLng(latitude, longitude),
                    radius = radius
                )
            ),
            languageCode = config.language
        )

        return retryTemplate.execute<SearchNearbyResponse, Exception> { context ->
            if (context.retryCount > 0) {
                logger.warn(
                    "Retrying Google Places API call - Attempt: {}/{}",
                    context.retryCount + 1,
                    config.maxRetries
                )
            }

            try {
                val sample = Timer.start()
                apiCallCounter.increment()

                val response = kotlinx.coroutines.runBlocking {
                    webClient
                        .post()
                        .uri("/v1/places:searchNearby")
                        .header("X-Goog-FieldMask", FIELD_MASK)
                        .bodyValue(request)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError) { response ->
                            when (response.statusCode().value()) {
                                429 -> {
                                    apiErrorCounter.increment()
                                    logger.error("Google API rate limit exceeded")
                                    throw BusinessException(ErrorCode.GOOGLE_API_RATE_LIMIT)
                                }
                                else -> {
                                    apiErrorCounter.increment()
                                    logger.error("Google API 4xx error: {}", response.statusCode())
                                    throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)
                                }
                            }
                        }
                        .onStatus(HttpStatusCode::is5xxServerError) {
                            apiErrorCounter.increment()
                            logger.error("Google API 5xx error: {}", it.statusCode())
                            throw BusinessException(ErrorCode.GOOGLE_API_ERROR)
                        }
                        .bodyToMono<SearchNearbyResponse>()
                        .awaitSingle()
                }

                sample.stop(apiTimer)

                logger.debug(
                    "Google Places API success - Found {} places at ({}, {})",
                    response.places?.size ?: 0,
                    latitude,
                    longitude
                )

                response

            } catch (e: BusinessException) {
                throw e
            } catch (e: Exception) {
                apiErrorCounter.increment()
                logger.error("Google API request failed", e)
                throw BusinessException(ErrorCode.GOOGLE_API_ERROR, cause = e)
            }
        }
    }
}
