package com.jigeumopen.jigeum.common.config

import com.jigeumopen.jigeum.common.interceptor.AdminAuthInterceptor
import com.jigeumopen.jigeum.common.interceptor.LoggingInterceptor
import com.jigeumopen.jigeum.common.interceptor.RateLimitingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val loggingInterceptor: LoggingInterceptor,
    private val rateLimitingInterceptor: RateLimitingInterceptor,
    private val adminAuthInterceptor: AdminAuthInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Admin API 인증 (최우선)
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api/v1/admin/**")
            .order(0)

        // Rate Limiting 적용 (Admin API 제외)
        registry.addInterceptor(rateLimitingInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/v1/admin/**")
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
