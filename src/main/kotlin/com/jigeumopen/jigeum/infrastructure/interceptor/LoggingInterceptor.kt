package com.jigeumopen.jigeum.infrastructure.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import java.util.*

@Component
class LoggingInterceptor : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val REQUEST_ID_HEADER = "X-Request-Id"
        private const val START_TIME_ATTRIBUTE = "startTime"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val startTime = System.currentTimeMillis()
        request.setAttribute(START_TIME_ATTRIBUTE, startTime)

        val requestId = request.getHeader(REQUEST_ID_HEADER) ?: UUID.randomUUID().toString()
        response.setHeader(REQUEST_ID_HEADER, requestId)

        logger.info(
            "==> REQUEST [{}] {} {} - QueryString: {} - RemoteAddr: {}",
            requestId,
            request.method,
            request.requestURI,
            request.queryString ?: "없음",
            getClientIp(request)
        )

        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startTime = request.getAttribute(START_TIME_ATTRIBUTE) as? Long ?: 0
        val elapsedTime = System.currentTimeMillis() - startTime
        val requestId = response.getHeader(REQUEST_ID_HEADER)

        if (ex != null) {
            logger.error(
                "<== RESPONSE [{}] {} {} - Status: {} - Time: {}ms - Error: {}",
                requestId,
                request.method,
                request.requestURI,
                response.status,
                elapsedTime,
                ex.message
            )
        } else {
            logger.info(
                "<== RESPONSE [{}] {} {} - Status: {} - Time: {}ms",
                requestId,
                request.method,
                request.requestURI,
                response.status,
                elapsedTime
            )
        }

        if (elapsedTime > 3000) {
            logger.warn(
                "SLOW API [{}] {} {} - {}ms",
                requestId,
                request.method,
                request.requestURI,
                elapsedTime
            )
        }
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr
    }
}
