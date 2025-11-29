package com.jigeumopen.jigeum.common.interceptor

import com.jigeumopen.jigeum.common.exception.BusinessException
import com.jigeumopen.jigeum.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminAuthInterceptor(
    @Value("\${admin.api.key}") private val adminApiKey: String
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val API_KEY_HEADER = "X-Admin-API-Key"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val requestApiKey = request.getHeader(API_KEY_HEADER)

        if (requestApiKey.isNullOrBlank()) {
            logger.warn("Admin API accessed without API key - IP: {}, URI: {}", request.remoteAddr, request.requestURI)
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }

        if (requestApiKey != adminApiKey) {
            logger.error("Admin API accessed with invalid API key - IP: {}, URI: {}", request.remoteAddr, request.requestURI)
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        logger.debug("Admin API access authorized - IP: {}, URI: {}", request.remoteAddr, request.requestURI)
        return true
    }
}
