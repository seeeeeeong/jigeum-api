package com.jigeumopen.jigeum.api.controller

import com.jigeumopen.jigeum.api.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.api.dto.response.ApiResponse
import com.jigeumopen.jigeum.api.dto.response.CafeResponse
import com.jigeumopen.jigeum.api.dto.response.PageResponse
import com.jigeumopen.jigeum.common.constants.CafeConstants.Api.BASE_PATH
import com.jigeumopen.jigeum.service.CafeSearchService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import kotlin.system.measureTimeMillis

@RestController
@RequestMapping("$BASE_PATH/cafes")
@Validated
class CafeSearchController(
    private val searchService: CafeSearchService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/search")
    fun searchCafes(@Valid request: SearchCafeRequest): ApiResponse<PageResponse<CafeResponse>> {
        logger.info("카페 검색: lat=${request.lat}, lng=${request.lng}, radius=${request.radius}m")

        val result: PageResponse<CafeResponse>
        val elapsedTime = measureTimeMillis {
            result = searchService.searchNearbyOpenCafes(request)
        }

        logger.info("검색 완료: ${result.content.size}개 결과, 소요시간: ${elapsedTime}ms")

        return ApiResponse.success(
            data = result,
            metadata = mapOf(
                "searchTime" to "${elapsedTime}ms",
                "query" to mapOf(
                    "location" to "${request.lat},${request.lng}",
                    "radius" to request.radius,
                    "time" to request.time
                )
            )
        )
    }

    @GetMapping("/search/name")
    fun searchByName(
        @RequestParam(required = true) keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<CafeResponse>> {
        logger.info("카페명 검색: keyword=$keyword")
        val result = searchService.searchByName(keyword, page, size)
        return ApiResponse.success(result)
    }

    @GetMapping("/search/category")
    fun searchByCategory(
        @RequestParam category: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<CafeResponse>> {
        logger.info("카테고리 검색: category=$category")
        val result = searchService.searchByCategory(category, page, size)
        return ApiResponse.success(result)
    }
}
