package com.jigeumopen.jigeum.cafe.client

import com.jigeumopen.jigeum.cafe.dto.*
import com.jigeumopen.jigeum.common.config.GooglePlacesConfig
import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class GooglePlacesClient(
    private val config: GooglePlacesConfig,
    webClientBuilder: WebClient.Builder
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val webClient = webClientBuilder
        .baseUrl(config.baseUrl)
        .codecs { it.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
        .build()

    companion object {
        private val FIELD_MASKS = listOf(
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.nationalPhoneNumber",
            "places.regularOpeningHours",
            "places.rating",
            "places.types",
            "places.userRatingCount"
        )
        private val FIELD_MASK = FIELD_MASKS.joinToString(",")
    }

    fun searchNearbyCafesReactive(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Mono<SearchNearbyResponse> {
        validateCoordinates(latitude, longitude)
        validateRadius(radius)

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

        return executeRequest {
            webClient.post()
                .uri("/v1/places:searchNearby")
                .header("X-Goog-Api-Key", config.apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SearchNearbyResponse::class.java)
        }.doOnSuccess { response ->
            logger.debug("Successfully fetched ${response.places?.size ?: 0} cafes near ($latitude, $longitude)")
        }
    }

    fun searchNearbyCafes(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): SearchNearbyResponse {
        return searchNearbyCafesReactive(latitude, longitude, radius)
            .block() ?: throw BusinessException(ErrorCode.GOOGLE_API_ERROR)
    }

    private fun <T> executeRequest(request: () -> Mono<T>): Mono<T> {
        return request()
            .timeout(Duration.ofSeconds(config.timeoutSeconds))
            .retryWhen(createRetrySpec())
            .onErrorMap(::mapToBusinessException)
    }

    private fun createRetrySpec(): Retry {
        return Retry.backoff(config.maxRetries.toLong(), Duration.ofSeconds(1))
            .maxBackoff(Duration.ofSeconds(10))
            .filter(::shouldRetry)
            .doBeforeRetry { signal ->
                logger.warn(
                    "Retrying Google API request (attempt ${signal.totalRetries() + 1}/${config.maxRetries}): ${signal.failure()?.message}"
                )
            }
            .onRetryExhaustedThrow { _, signal ->
                BusinessException(ErrorCode.GOOGLE_API_RATE_LIMIT, signal.failure())
            }
    }

    private fun shouldRetry(throwable: Throwable): Boolean {
        return when (throwable) {
            is WebClientResponseException -> {
                throwable.statusCode.is5xxServerError || throwable.statusCode.value() == 429
            }

            else -> false
        }
    }

    private fun mapToBusinessException(error: Throwable): Throwable {
        return when (error) {
            is BusinessException -> error
            is WebClientResponseException -> handleWebClientError(error)
            else -> BusinessException(ErrorCode.EXTERNAL_API_ERROR, error)
        }
    }

    private fun handleWebClientError(error: WebClientResponseException): BusinessException {
        val statusCode = error.statusCode.value()
        val responseBody = error.responseBodyAsString

        logger.error("Google Places API error: status=$statusCode, body=$responseBody")

        return when (statusCode) {
            400 -> BusinessException(ErrorCode.INVALID_PARAMETER, error)
            401, 403 -> BusinessException(ErrorCode.INVALID_API_KEY, error)
            404 -> BusinessException(ErrorCode.RESOURCE_NOT_FOUND, error)
            429 -> BusinessException(ErrorCode.GOOGLE_API_RATE_LIMIT, error)
            in 500..599 -> BusinessException(ErrorCode.GOOGLE_API_ERROR, error)
            else -> BusinessException(ErrorCode.EXTERNAL_API_ERROR, error)
        }
    }

    private fun validateCoordinates(latitude: Double, longitude: Double) {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            throw BusinessException(ErrorCode.INVALID_COORDINATE)
        }
    }

    private fun validateRadius(radius: Double) {
        if (radius <= 0 || radius > 50000) {
            throw BusinessException(ErrorCode.INVALID_SEARCH_RADIUS)
        }
    }
}
