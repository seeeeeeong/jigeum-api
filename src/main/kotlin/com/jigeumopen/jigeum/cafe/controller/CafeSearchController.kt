package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.ApiResponse
import com.jigeumopen.jigeum.cafe.service.CafeSearchService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cafes")
class CafeSearchController(
    private val searchService: CafeSearchService
) {
    @GetMapping("/search")
    fun search(@Valid @ModelAttribute request: SearchCafeRequest) =
        ApiResponse.success(searchService.searchNearby(request))
}
