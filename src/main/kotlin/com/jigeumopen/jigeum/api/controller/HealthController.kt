package com.jigeumopen.jigeum.api.controller

import org.springframework.boot.info.BuildProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
class HealthController(
    private val buildProperties: BuildProperties? = null
) {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "timestamp" to LocalDateTime.now(),
                "service" to "jigeum-api",
                "version" to (buildProperties?.version ?: "unknown")
            )
        )
    }

    @GetMapping("/info")
    fun info(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "name" to "지금영업중 API",
                "description" to "위치 기반 카페 검색 서비스",
                "version" to (buildProperties?.version ?: "0.0.1-SNAPSHOT"),
                "build" to mapOf(
                    "time" to (buildProperties?.time ?: "unknown"),
                    "artifact" to (buildProperties?.artifact ?: "jigeum-api"),
                    "group" to (buildProperties?.group ?: "com.jigeumopen")
                )
            )
        )
    }
}
