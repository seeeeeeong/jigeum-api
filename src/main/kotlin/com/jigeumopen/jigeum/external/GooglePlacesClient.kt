package com.jigeumopen.jigeum.external

import com.jigeumopen.jigeum.external.config.GooglePlacesConfig
import com.jigeumopen.jigeum.external.dto.*
import com.jigeumopen.jigeum.external.exception.GooglePlacesException
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
    private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun searchNearbyCafes(latitude: Double, longitude: Double, radius: Double): Mono<SearchNearbyResponse> {
        val request = SearchNearbyRequest(
            includedTypes = listOf("cafe"),
            maxResultCount = 20,
            locationRestriction = LocationRestriction(
                circle = Circle(
                    center = LatLng(latitude, longitude),
                    radius = radius
                )
            ),
            languageCode = "ko"
        )

        return executeRequest {
            webClient.post()
                .uri("/v1/places:searchNearby")
                .header("X-Goog-Api-Key", config.apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SearchNearbyResponse::class.java)
                .doOnSuccess { response ->
                    logger.debug("API 응답: places size = ${response.places?.size ?: 0}")
                    logger.debug("첫 번째 카페: ${response.places?.firstOrNull()?.displayName?.text}")
                }
        }
    }

    private fun <T> executeRequest(request: () -> Mono<T>): Mono<T> {
        return request()
            .timeout(Duration.ofSeconds(config.timeoutSeconds))
            .retryWhen(
                Retry.backoff(config.maxRetries, Duration.ofSeconds(1))
                    .filter { it !is WebClientResponseException || it.statusCode.is5xxServerError }
                    .doBeforeRetry { signal ->
                        logger.warn("Retrying request, attempt: ${signal.totalRetries() + 1}")
                    }
            )
            .onErrorMap { error ->
                when (error) {
                    is WebClientResponseException -> handleWebClientError(error)
                    else -> GooglePlacesException.UnknownError("Unexpected error occurred", error)
                }
            }
    }

    private fun handleWebClientError(error: WebClientResponseException): GooglePlacesException {
        return when (error.statusCode.value()) {
            400 -> GooglePlacesException.BadRequest("Invalid request parameters", error)
            401, 403 -> GooglePlacesException.Unauthorized("API key is invalid or unauthorized", error)
            404 -> GooglePlacesException.NotFound("Resource not found", error)
            429 -> GooglePlacesException.RateLimited("API rate limit exceeded", error)
            in 500..599 -> GooglePlacesException.ServerError("Google Places API server error", error)
            else -> GooglePlacesException.UnknownError("Unknown error: ${error.message}", error)
        }
    }

    companion object {
        private const val FIELD_MASK = "places.id,places.displayName,places.formattedAddress," +
                "places.location,places.nationalPhoneNumber,places.regularOpeningHours,places.rating"
    }
}
