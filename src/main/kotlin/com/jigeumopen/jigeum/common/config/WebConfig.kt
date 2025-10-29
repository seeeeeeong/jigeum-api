package com.jigeumopen.jigeum.common.config

import com.jigeumopen.jigeum.common.interceptor.LoggingInterceptor
import com.jigeumopen.jigeum.common.interceptor.RateLimitingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val loggingInterceptor: LoggingInterceptor,
    private val rateLimitingInterceptor: RateLimitingInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Rate Limiting 먼저 적용
        registry.addInterceptor(rateLimitingInterceptor)
            .addPathPatterns("/api/**")
            .order(1)
        
        // 로깅은 나중에
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/api/**")
            .order(2)
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                "http://localhost:3000",
                "http://localhost:8080",
                "https://jigeum.com"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
