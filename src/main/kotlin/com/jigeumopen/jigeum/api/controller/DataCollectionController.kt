package com.jigeumopen.jigeum.api.controller

import com.jigeumopen.jigeum.api.dto.response.ApiResponse
import com.jigeumopen.jigeum.api.exception.BusinessException
import com.jigeumopen.jigeum.api.exception.ErrorCode
import com.jigeumopen.jigeum.common.constants.CafeConstants.Api.BASE_PATH
import com.jigeumopen.jigeum.service.DataCollectionService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import kotlin.system.measureTimeMillis

@RestController
@RequestMapping("$BASE_PATH/admin/collect")
@Validated
class DataCollectionController(
    private val collectionService: DataCollectionService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/area")
    fun collectAreaData(
        @RequestParam @Min(-90) @Max(90) lat: Double,
        @RequestParam @Min(-180) @Max(180) lng: Double,
        @RequestParam(defaultValue = "5000") @Min(100) @Max(50000) radius: Int,
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?
    ): ApiResponse<Map<String, Any>> {
        validateAdminKey(adminKey)

        logger.info("지역 데이터 수집 시작: lat=$lat, lng=$lng, radius=${radius}m")

        val savedCount: Int
        val elapsedTime = measureTimeMillis {
            savedCount = collectionService.collectCafesInArea(lat, lng, radius)
        }

        logger.info("수집 완료: ${savedCount}개 저장, 소요시간: ${elapsedTime}ms")

        return ApiResponse.success(
            data = mapOf(
                "savedCount" to savedCount,
                "location" to "$lat,$lng",
                "radius" to radius
            ),
            metadata = mapOf(
                "collectionTime" to "${elapsedTime}ms"
            )
        )
    }

    @PostMapping("/seoul")
    fun collectSeoulData(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?
    ): ApiResponse<Map<String, Any>> {
        validateAdminKey(adminKey)

        logger.info("서울 전역 데이터 수집 시작")

        val results: Map<String, Int>
        val elapsedTime = measureTimeMillis {
            results = collectionService.collectAllSeoulCafes()
        }

        val totalCount = results.values.sum()
        logger.info("서울 전역 수집 완료: 총 ${totalCount}개, 소요시간: ${elapsedTime / 1000}초")

        return ApiResponse.success(
            data = mapOf(
                "results" to results,
                "totalCount" to totalCount,
                "locations" to results.size
            ),
            metadata = mapOf(
                "collectionTime" to "${elapsedTime / 1000}s"
            )
        )
    }

    @GetMapping("/status")
    fun getCollectionStatus(
        @RequestHeader("X-Admin-Key", required = false) adminKey: String?
    ): ApiResponse<Map<String, Any>> {
        validateAdminKey(adminKey)

        val status = collectionService.getCollectionStatus()
        return ApiResponse.success(status)
    }

    private fun validateAdminKey(adminKey: String?) {
=        val validKey = System.getenv("ADMIN_API_KEY") ?: "admin-secret-key"
        if (adminKey != validKey) {
            throw BusinessException(ErrorCode.ADMIN_ONLY)
        }
    }
}
