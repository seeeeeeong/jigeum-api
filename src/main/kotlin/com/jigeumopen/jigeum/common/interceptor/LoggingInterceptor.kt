package com.jigeumopen.jigeum.common.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class LoggingInterceptor : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(req: HttpServletRequest, res: HttpServletResponse, handler: Any): Boolean {
        req.setAttribute("startTime", System.currentTimeMillis())
        return true
    }

    override fun afterCompletion(req: HttpServletRequest, res: HttpServletResponse, handler: Any, ex: Exception?) {
        val start = req.getAttribute("startTime") as? Long ?: return
        val elapsed = System.currentTimeMillis() - start
        val msg = "${req.method} ${req.requestURI} - ${res.status} - ${elapsed}ms"

        when {
            ex != null -> log.error(msg, ex)
            elapsed > 3000 -> log.warn("$msg [SLOW]")
            else -> log.info(msg)
        }
    }
}
