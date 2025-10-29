package com.jigeumopen.jigeum.common.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration

@Component
class RateLimitingInterceptor(
    private val redisTemplate: RedisTemplate<String, Any>
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RATE_LIMIT_PREFIX = "rate_limit:"
        private const val MAX_REQUESTS = 100
        private const val WINDOW_SECONDS = 60L
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {

        if (request.requestURI.contains("/admin/")) {
            return true
        }

        val clientIp = getClientIp(request)
        val key = "$RATE_LIMIT_PREFIX$clientIp"

        val operations = redisTemplate.opsForValue()
        val current = operations.increment(key) ?: 0L

        if (current == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS))
        }

        val remaining = (MAX_REQUESTS - current).coerceAtLeast(0)

        response.addHeader("X-RateLimit-Limit", MAX_REQUESTS.toString())
        response.addHeader("X-RateLimit-Remaining", remaining.toString())
        response.addHeader("X-RateLimit-Reset", WINDOW_SECONDS.toString())

        if (current > MAX_REQUESTS) {
            logger.warn("Rate limit exceeded for IP: $clientIp")
            response.status = 429
            response.contentType = "application/json"
            response.writer.write(
                """{"success":false,"message":"요청 제한을 초과했습니다. 잠시 후 다시 시도해주세요.","timestamp":"${java.time.LocalDateTime.now()}"}"""
            )
            return false
        }

        return true
    }

    private fun getClientIp(request: HttpServletRequest): String {
        return request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.getHeader("X-Real-IP")
            ?: request.remoteAddr
    }
}
