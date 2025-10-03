package com.jigeumopen.jigeum.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationProperties(prefix = "google.places")
data class GooglePlacesConfig(
    var apiKey: String = "",
    var baseUrl: String = "https://places.googleapis.com",  // 수정!
    var detailFields: String = "name,formatted_address,formatted_phone_number,geometry,opening_hours,rating",
    var language: String = "ko",
    var timeoutSeconds: Long = 10,
    var maxRetries: Long = 3
) {
    @Bean
    fun googlePlacesWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .build()
    }
}
