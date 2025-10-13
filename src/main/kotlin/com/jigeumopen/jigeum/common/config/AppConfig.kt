package com.jigeumopen.jigeum.common.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableJpaAuditing
@EnableConfigurationProperties(GooglePlacesConfig::class)
class AppConfig {

    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().apply {

        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())

        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("http://localhost:*")
            allowedMethods = listOf("GET", "POST", "OPTIONS", "DELETE", "PUT")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", config)
        }

        return CorsWebFilter(source)
    }
}
