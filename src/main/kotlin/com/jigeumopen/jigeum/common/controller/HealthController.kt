package com.jigeumopen.jigeum.common.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    
    @GetMapping("/health")
    fun health() = mapOf("status" to "UP")
}
