package com.jigeumopen.jigeum.api.controller

import com.jigeumopen.jigeum.api.dto.request.CreateCafeRequest
import com.jigeumopen.jigeum.api.dto.response.ApiResponse
import com.jigeumopen.jigeum.api.dto.response.CafeResponse
import com.jigeumopen.jigeum.common.constants.CafeConstants.Api.BASE_PATH
import com.jigeumopen.jigeum.service.CafeService
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.hibernate.annotations.Parameter
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("$BASE_PATH/cafes")
@Validated
class CafeController(
    private val cafeService: CafeService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getAllCafes(): ApiResponse<List<CafeResponse>> {
        logger.info("전체 카페 조회 요청")
        val cafes = cafeService.getAllCafes()
        return ApiResponse.success(cafes, "카페 목록 조회 성공")
    }

    @GetMapping("/{id}")
    fun getCafe(
        @PathVariable @Min(1) id: Long
    ): ApiResponse<CafeResponse> {
        logger.info("카페 단건 조회: id=$id")
        val cafe = cafeService.getCafe(id)
        return ApiResponse.success(cafe)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCafe(
        @Valid @RequestBody request: CreateCafeRequest
    ): ApiResponse<CafeResponse> {
        logger.info("카페 등록: name=${request.name}")
        val cafe = cafeService.createCafe(request)
        return ApiResponse.success(cafe, "카페 등록 성공")
    }

    @PutMapping("/{id}")
    fun updateCafe(
        @PathVariable @Min(1) id: Long,
        @Valid @RequestBody request: CreateCafeRequest
    ): ApiResponse<CafeResponse> {
        logger.info("카페 수정: id=$id")
        val cafe = cafeService.updateCafe(id, request)
        return ApiResponse.success(cafe, "카페 수정 성공")
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCafe(
        @PathVariable @Min(1) id: Long
    ): ApiResponse<Unit> {
        logger.info("카페 삭제: id=$id")
        cafeService.deleteCafe(id)
        return ApiResponse.success(Unit, "카페 삭제 성공")
    }
}
