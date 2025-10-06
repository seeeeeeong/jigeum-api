package com.jigeumopen.jigeum.cafe.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jigeumopen.jigeum.cafe.dto.*
import com.jigeumopen.jigeum.common.config.GooglePlacesConfig
import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class GooglePlacesClient(
    private val config: GooglePlacesConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(config.timeoutSeconds))
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

    fun searchNearbyCafes(latitude: Double, longitude: Double, radius: Double): SearchNearbyResponse {
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

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("${config.baseUrl}/v1/places:searchNearby"))
            .header("X-Goog-Api-Key", config.apiKey)
            .header("X-Goog-FieldMask", FIELD_MASK)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
            .timeout(Duration.ofSeconds(config.timeoutSeconds))
            .build()

        return try {
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

            when (response.statusCode()) {
                200 -> objectMapper.readValue<SearchNearbyResponse>(response.body())
                429 -> throw BusinessException(ErrorCode.GOOGLE_API_RATE_LIMIT)
                in 500..599 -> throw BusinessException(ErrorCode.GOOGLE_API_ERROR)
                else -> {
                    logger.error("Google API error: ${response.statusCode()} - ${response.body()}")
                    throw BusinessException(ErrorCode.EXTERNAL_API_ERROR)
                }
            }
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            logger.error("Google API request failed", e)
            throw BusinessException(ErrorCode.GOOGLE_API_ERROR, e)
        }
    }
}
