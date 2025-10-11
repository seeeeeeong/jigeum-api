package com.jigeumopen.jigeum.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "google.places")
data class GooglePlacesConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://places.googleapis.com",
    val language: String = "ko",
    val timeoutSeconds: Long = 10,
    val maxRetries: Int = 3
)
