package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.CafeRequest
import com.jigeumopen.jigeum.cafe.service.CafeService
import com.jigeumopen.jigeum.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cafes")
class CafeController(
    private val searchService: CafeService
) {
    @GetMapping("/search")
    suspend fun searchCafes(@Valid @ModelAttribute request: CafeRequest) =
        ApiResponse.success(searchService.searchNearby(request))
}
