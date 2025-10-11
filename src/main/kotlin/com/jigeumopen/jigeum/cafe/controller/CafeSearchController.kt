package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.ApiResponse
import com.jigeumopen.jigeum.cafe.service.CafeSearchService
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cafes")
class CafeSearchController(
    private val searchService: CafeSearchService
) {
    @GetMapping("/search")
    fun searchCafes(
        @Valid @ModelAttribute request: SearchCafeRequest
    ) = runBlocking {
        ApiResponse.success(searchService.searchNearby(request))
    }
}
