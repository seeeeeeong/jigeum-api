package com.jigeumopen.jigeum.common.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class LoggingInterceptor : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val START_TIME = "startTime"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        request.setAttribute(START_TIME, System.currentTimeMillis())
        logger.info("==> ${request.method} ${request.requestURI}")
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startTime = request.getAttribute(START_TIME) as? Long ?: return
        val elapsed = System.currentTimeMillis() - startTime

        val logMsg = "<== ${request.method} ${request.requestURI} - ${response.status} - ${elapsed}ms"

        if (ex != null) {
            logger.error("$logMsg - Error: ${ex.message}")
        } else if (elapsed > 3000) {
            logger.warn("$logMsg [SLOW]")
        } else {
            logger.info(logMsg)
        }
    }
}
