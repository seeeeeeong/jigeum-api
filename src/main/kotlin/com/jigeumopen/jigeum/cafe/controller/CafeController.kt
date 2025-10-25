package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.CafeRequest
import com.jigeumopen.jigeum.cafe.service.CafeService
import com.jigeumopen.jigeum.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cafes")
class CafeController(
    private val cafeService: CafeService
) {
    @GetMapping("/search")
    suspend fun searchCafes(@Valid @ModelAttribute request: CafeRequest) =
        ApiResponse.success(cafeService.searchNearby(request))

    @GetMapping("/{cafeId}")
    suspend fun getCafeDetail(@PathVariable cafeId: Long) =
        ApiResponse.success(cafeService.getCafeDetail(cafeId))
}
