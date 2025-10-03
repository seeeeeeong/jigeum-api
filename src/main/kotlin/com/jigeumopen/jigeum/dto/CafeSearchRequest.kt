package com.jigeumopen.jigeum.external.dto

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

data class CafeSearchRequest(
    val latitude: Double,
    val longitude: Double,
    val radius: Int = DEFAULT_RADIUS,
    val type: String = "cafe",
    val language: String = "ko"
) {
    init {
        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)
        require(radius in MIN_RADIUS..MAX_RADIUS)
    }

    fun toQueryParams(): MultiValueMap<String, String> {
        return LinkedMultiValueMap<String, String>().apply {
            add("location", "$latitude,$longitude")
            add("radius", radius.toString())
            add("type", type)
            add("language", language)
        }
    }

    companion object {
        const val DEFAULT_RADIUS = 5000
        const val MIN_RADIUS = 1
        const val MAX_RADIUS = 50000
    }
}
