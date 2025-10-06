package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.response.ApiResponse
import com.jigeumopen.jigeum.cafe.dto.response.AreaCollectionResponse
import com.jigeumopen.jigeum.cafe.dto.response.BatchCollectionResponse
import com.jigeumopen.jigeum.cafe.service.DataCollectionService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/collect")
@Validated
class DataCollectionController(
    private val collectionService: DataCollectionService
) {

    @PostMapping("/area")
    fun collectArea(
        @RequestParam @Min(-90) @Max(90) lat: Double,
        @RequestParam @Min(-180) @Max(180) lng: Double,
        @RequestParam(defaultValue = "5000") @Min(100) @Max(50000) radius: Int
    ): ApiResponse<AreaCollectionResponse> =
        ApiResponse.success(collectionService.collectCafesInArea(lat, lng, radius))

    @PostMapping("/seoul")
    fun collectSeoul(): ApiResponse<BatchCollectionResponse> =
        ApiResponse.success(collectionService.collectAllCafes())
}
