package com.jigeumopen.jigeum.common.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import kotlin.system.measureTimeMillis

@Aspect
@Component
class LoggingAspect {

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    fun controllerMethods() {
    }

    @Pointcut("@within(org.springframework.stereotype.Service)")
    fun serviceMethods() {
    }

    @Around("controllerMethods()")
    fun logController(joinPoint: ProceedingJoinPoint): Any? {
        val logger = LoggerFactory.getLogger(joinPoint.target.javaClass)
        val methodName = joinPoint.signature.name
        val className = joinPoint.target.javaClass.simpleName

        val request =
            (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        val requestInfo = "${request.method} ${request.requestURI}"

        logger.debug("==> [$className.$methodName] $requestInfo")

        val result: Any?
        val elapsed = measureTimeMillis {
            result = joinPoint.proceed()
        }

        logger.debug("<== [$className.$methodName] ${elapsed}ms")

        if (elapsed > 1000) {
            logger.warn("SLOW API: [$className.$methodName] ${elapsed}ms")
        }

        return result
    }

    @Around("serviceMethods()")
    fun logService(joinPoint: ProceedingJoinPoint): Any? {
        val logger = LoggerFactory.getLogger(joinPoint.target.javaClass)
        val methodName = joinPoint.signature.name
        val className = joinPoint.target.javaClass.simpleName

        logger.trace("[$className.$methodName] START")

        val result: Any?
        val elapsed = measureTimeMillis {
            result = joinPoint.proceed()
        }

        logger.trace("[$className.$methodName] END - ${elapsed}ms")

        return result
    }
}
