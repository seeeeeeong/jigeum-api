package com.jigeumopen.jigeum.api.controller

import com.jigeumopen.jigeum.api.dto.response.ApiResponse
import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.common.constants.CafeConstants.Api.BASE_PATH
import com.jigeumopen.jigeum.service.DataCollectionService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.beans.factory.annotation.Value
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_PATH/admin/collect")
@Validated
class DataCollectionController(
    private val collectionService: DataCollectionService,
    @Value("\${admin.api.key:admin-secret-key}")
    private val adminApiKey: String
) {
    @PostMapping("/area")
    fun collectAreaData(
        @RequestParam @Min(-90) @Max(90) lat: Double,
        @RequestParam @Min(-180) @Max(180) lng: Double,
        @RequestParam(defaultValue = "5000") @Min(100) @Max(50000) radius: Int,
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?
    ): ApiResponse<Map<String, Any>> {
        validateAdminKey(adminKey)

        val savedCount = collectionService.collectCafesInArea(lat, lng, radius)
        return ApiResponse.success(
            data = mapOf(
                "savedCount" to savedCount,
                "location" to "$lat,$lng",
                "radius" to radius
            )
        )
    }

    @PostMapping("/seoul")
    fun collectSeoulData(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?
    ): ApiResponse<Map<String, Any>> {
        validateAdminKey(adminKey)

        val results = collectionService.collectAllSeoulCafes()
        val totalCount = results.values.sum()

        return ApiResponse.success(
            data = mapOf(
                "results" to results,
                "totalCount" to totalCount,
                "locations" to results.size
            )
        )
    }

    @GetMapping("/status")
    fun getCollectionStatus(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?
    ): ApiResponse<Map<String, Any>> {
        validateAdminKey(adminKey)
        return ApiResponse.success(collectionService.getCollectionStatus())
    }

    private fun validateAdminKey(adminKey: String?) {
        if (adminKey != adminApiKey) {
            throw BusinessException(ErrorCode.ADMIN_ONLY)
        }
    }
}
