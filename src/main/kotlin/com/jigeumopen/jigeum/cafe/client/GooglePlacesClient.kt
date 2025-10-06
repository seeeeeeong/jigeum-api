package com.jigeumopen.jigeum.cafe.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jigeumopen.jigeum.cafe.dto.*
import com.jigeumopen.jigeum.common.config.GooglePlacesConfig
import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class GooglePlacesClient(
    private val config: GooglePlacesConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(config.timeoutSeconds))
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

    fun searchNearbyCafes(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): SearchNearbyResponse {
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

        return executeWithRetry(config.maxRetries.toInt()) {
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("${config.baseUrl}/v1/places:searchNearby"))
                .header("X-Goog-Api-Key", config.apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requireNotNull(objectMapper.writeValueAsString(request))))
                .timeout(Duration.ofSeconds(config.timeoutSeconds))
                .build()

            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

            when (response.statusCode()) {
                200 -> objectMapper.readValue<SearchNearbyResponse>(response.body())
                400 -> throw BusinessException(ErrorCode.INVALID_PARAMETER, response.body())
                401, 403 -> throw BusinessException(ErrorCode.INVALID_API_KEY, response.body())
                404 -> throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND, response.body())
                429 -> throw BusinessException(ErrorCode.GOOGLE_API_RATE_LIMIT, response.body())
                in 500..599 -> throw BusinessException(ErrorCode.GOOGLE_API_ERROR, response.body())
                else -> {
                    logger.error("Unexpected Google API response: status=${response.statusCode()}, body=${response.body()}")
                    throw BusinessException(ErrorCode.EXTERNAL_API_ERROR, response.body())
                }
            }
        }
    }

    private fun <T> executeWithRetry(maxRetries: Int, block: () -> T): T {
        var lastException: Throwable? = null

        for (attempt in 0 until maxRetries) {
            try {
                return block()
            } catch (e: BusinessException) {
                lastException = e
                if (e.errorCode in listOf(ErrorCode.GOOGLE_API_RATE_LIMIT, ErrorCode.GOOGLE_API_ERROR)) {
                    if (attempt < maxRetries - 1) {
                        val delay = minOf(1000L * (1 shl attempt), 10000L)
                        logger.warn("Retry attempt ${attempt + 1}/$maxRetries after ${delay}ms: ${e.message}")
                        TimeUnit.MILLISECONDS.sleep(delay)
                    }
                } else {
                    throw e
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delay = 1000L * (attempt + 1)
                    logger.warn("Network error, retry attempt ${attempt + 1}/$maxRetries after ${delay}ms", e)
                    TimeUnit.MILLISECONDS.sleep(delay)
                }
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delay = 1000L * (attempt + 1)
                    logger.warn("Timeout, retry attempt ${attempt + 1}/$maxRetries after ${delay}ms", e)
                    TimeUnit.MILLISECONDS.sleep(delay)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delay = 1000L * (attempt + 1)
                    logger.warn("Unknown error, retry attempt ${attempt + 1}/$maxRetries after ${delay}ms", e)
                    TimeUnit.MILLISECONDS.sleep(delay)
                }
            }
        }

        throw BusinessException(ErrorCode.GOOGLE_API_ERROR, requireNotNull(lastException))
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
