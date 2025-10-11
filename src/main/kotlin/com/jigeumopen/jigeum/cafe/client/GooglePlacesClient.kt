package com.jigeumopen.jigeum.cafe.client

import com.jigeumopen.jigeum.cafe.dto.*
import com.jigeumopen.jigeum.common.config.GooglePlacesConfig
import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class GooglePlacesClient(
    private val config: GooglePlacesConfig,
    webClientBuilder: WebClient.Builder
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val webClient = webClientBuilder
        .baseUrl(config.baseUrl)
        .defaultHeader("X-Goog-Api-Key", config.apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build()

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

        return try {
            webClient
                .post()
                .uri("/v1/places:searchNearby")
                .header("X-Goog-FieldMask", FIELD_MASK)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { response ->
                    when (response.statusCode().value()) {
                        429 -> throw BusinessException(ErrorCode.GOOGLE_API_RATE_LIMIT)
                        else -> throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)
                    }
                }
                .onStatus(HttpStatusCode::is5xxServerError) {
                    throw BusinessException(ErrorCode.GOOGLE_API_ERROR)
                }
                .bodyToMono<SearchNearbyResponse>()
                .awaitSingle()
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            logger.error("Google API request failed", e)
            throw BusinessException(ErrorCode.GOOGLE_API_ERROR)
        }
    }
}
