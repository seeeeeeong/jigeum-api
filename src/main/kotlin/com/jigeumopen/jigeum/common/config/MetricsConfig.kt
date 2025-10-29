package com.jigeumopen.jigeum.common.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig {

    @Value("\${spring.application.name}")
    private lateinit var applicationName: String

    @Value("\${spring.profiles.active:default}")
    private lateinit var activeProfile: String

    @Bean
    fun meterRegistryCustomizer(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config().commonTags(
                "application", applicationName,
                "environment", activeProfile
            )
        }
    }
}

suspend fun <T> Timer.recordSuspend(block: suspend () -> T): T {
    val sample = Timer.start()
    return try {
        block()
    } finally {
        sample.stop(this)
    }
}
