package com.jigeumopen.jigeum.common.config

import com.jigeumopen.jigeum.common.constants.CafeConstants.Api.BASE_PATH
import com.jigeumopen.jigeum.common.interceptor.AdminAuthInterceptor
import com.jigeumopen.jigeum.common.interceptor.LoggingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val loggingInterceptor: LoggingInterceptor,
    private val adminAuthInterceptor: AdminAuthInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/health",
                "/api/info"
            )

        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("$BASE_PATH/admin/**")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:8081"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
