package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.dto.response.ApiResponse
import com.jigeumopen.jigeum.cafe.service.CafeSearchService
import com.jigeumopen.jigeum.common.constants.CafeConstants.Api.BASE_PATH
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_PATH/cafes")
@Validated
class CafeSearchController(
    private val searchService: CafeSearchService
) {
    @GetMapping("/search")
    fun search(@Valid request: SearchCafeRequest) =
        ApiResponse.success(searchService.searchNearby(request))

    @GetMapping("/search/name")
    fun searchByName(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ApiResponse.success(searchService.searchByName(keyword, page, size))

    @GetMapping("/search/category")
    fun searchByCategory(
        @RequestParam category: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ApiResponse.success(searchService.searchByCategory(category, page, size))
}
