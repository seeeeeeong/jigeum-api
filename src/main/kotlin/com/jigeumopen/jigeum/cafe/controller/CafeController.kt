package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.ApiResponse
import com.jigeumopen.jigeum.cafe.dto.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.service.CafeService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cafes")
class CafeController(
    private val searchService: CafeService
) {
    @GetMapping("/search")
    suspend fun searchCafes(@Valid @ModelAttribute request: SearchCafeRequest) =
        ApiResponse.success(searchService.searchNearby(request))
}
