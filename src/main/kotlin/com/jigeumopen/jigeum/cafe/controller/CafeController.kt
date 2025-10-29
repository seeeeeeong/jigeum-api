package com.jigeumopen.jigeum.cafe.controller

import com.jigeumopen.jigeum.cafe.dto.CafeDetailResponse
import com.jigeumopen.jigeum.cafe.dto.CafeRequest
import com.jigeumopen.jigeum.cafe.dto.CafeResponse
import com.jigeumopen.jigeum.cafe.service.CafeService
import com.jigeumopen.jigeum.common.dto.ApiResponse
import com.jigeumopen.jigeum.common.dto.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cafes")
@Tag(name = "Cafe", description = "카페 검색 및 조회 API")
class CafeController(
    private val cafeService: CafeService
) {
    
    @GetMapping("/search")
    @Operation(
        summary = "주변 카페 검색",
        description = """
            현재 위치를 기준으로 지정된 반경 내에서 영업중인 카페를 검색합니다.
            
            - 검색 반경: 100m ~ 50km
            - 시간대별 영업 여부 필터링

            **예시:**
            - `/api/v1/cafes/search?lat=37.4979&lng=127.0276&radius=1000&time=14:00`
        """
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "검색 성공",
                content = [Content(schema = Schema(implementation = PageResponse::class))]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 파라미터 (위도/경도 범위, 반경 범위 등)"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "429",
                description = "요청 제한 초과 (분당 100회)"
            )
        ]
    )
    suspend fun searchCafes(
        @Parameter(description = "위도 (-90 ~ 90)", example = "37.4979", required = true)
        @Valid @ModelAttribute request: CafeRequest
    ): ApiResponse<PageResponse<CafeResponse>> =
        ApiResponse.success(cafeService.searchNearby(request))

    @GetMapping("/{cafeId}")
    @Operation(
        summary = "카페 상세 정보 조회",
        description = """
            카페의 상세 정보와 요일별 운영시간을 조회합니다.
            
            **포함 정보:**
            - 기본 정보 (이름, 주소, 위치)
            - 요일별 운영시간 (월~일)
            
            **예시:**
            - `/api/v1/cafes/1`
        """
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = CafeDetailResponse::class))]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "카페를 찾을 수 없음"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "429",
                description = "요청 제한 초과"
            )
        ]
    )
    suspend fun getCafeDetail(
        @Parameter(description = "카페 ID", example = "1", required = true)
        @PathVariable cafeId: Long
    ): ApiResponse<CafeDetailResponse> =
        ApiResponse.success(cafeService.getCafeDetail(cafeId))
}
