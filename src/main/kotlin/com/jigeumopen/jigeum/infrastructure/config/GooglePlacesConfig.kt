package com.jigeumopen.jigeum.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationProperties(prefix = "google.places")
data class GooglePlacesConfig(
    var apiKey: String = "",
    var baseUrl: String = "https://places.googleapis.com",
    var detailFields: String = "name,formatted_address,formatted_phone_number,geometry,opening_hours,rating",
    var language: String = "ko",
    var timeoutSeconds: Long = 10,
    var maxRetries: Long = 3
) {
    @Bean
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }
}
