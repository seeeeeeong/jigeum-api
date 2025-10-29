package com.jigeumopen.jigeum.batch.controller

import com.jigeumopen.jigeum.batch.dto.BatchStatistics
import com.jigeumopen.jigeum.batch.dto.JobTypeStatistics
import com.jigeumopen.jigeum.batch.dto.OperationResponse
import com.jigeumopen.jigeum.batch.entity.JobType
import com.jigeumopen.jigeum.batch.service.*
import com.jigeumopen.jigeum.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/batch")
@Tag(name = "Batch Admin", description = "배치 API (ADMIN)")
class BatchController(
    private val rawDataCollectionService: RawDataCollectionService,
    private val dataProcessingService: DataProcessingService,
    private val batchJobService: BatchJobService,
    private val batchMetricsService: BatchMetricsService
) {

    @PostMapping("/collect")
    @Operation(
        summary = "원본 데이터 수집",
        description = "Google Places API를 통해 서울 전역의 카페 데이터를 수집합니다."
    )
    suspend fun collectRawData(): ApiResponse<OperationResponse> =
        ApiResponse.success(rawDataCollectionService.collectRawData())

    @PostMapping("/process")
    @Operation(
        summary = "원본 데이터 처리",
        description = "수집된 원본 데이터를 가공하여 서비스 DB에 저장합니다."
    )
    suspend fun processRawData(
        @Parameter(description = "모든 데이터 재처리 여부")
        @RequestParam(defaultValue = "false") reprocessAll: Boolean
    ): ApiResponse<OperationResponse> =
        ApiResponse.success(dataProcessingService.processRawData(reprocessAll))

    @GetMapping("/jobs/{batchId}")
    @Operation(
        summary = "배치 작업 조회",
        description = "특정 배치 작업의 상세 정보를 조회합니다."
    )
    fun getBatchJob(
        @Parameter(description = "배치 작업 ID")
        @PathVariable batchId: String
    ): ApiResponse<OperationResponse> =
        ApiResponse.success(batchJobService.getBatchJob(batchId))

    @PostMapping("/cleanup")
    @Operation(
        summary = "장애 작업 정리",
        description = "6시간 이상 실행중인 작업을 실패 처리합니다."
    )
    fun cleanupStuckJobs(): ApiResponse<String> =
        ApiResponse.success(batchJobService.cleanupStuckJobs())

    @GetMapping("/statistics")
    @Operation(
        summary = "배치 작업 통계",
        description = "최근 24시간 배치 작업 통계를 조회합니다."
    )
    fun getBatchStatistics(): ApiResponse<BatchStatistics> =
        ApiResponse.success(batchMetricsService.getBatchStatistics())

    @GetMapping("/statistics/{jobType}")
    @Operation(
        summary = "작업 타입별 통계",
        description = "특정 작업 타입의 통계를 조회합니다."
    )
    fun getJobTypeStatistics(
        @Parameter(description = "작업 타입 (COLLECT_RAW_DATA, PROCESS_RAW_DATA)")
        @PathVariable jobType: JobType
    ): ApiResponse<JobTypeStatistics> =
        ApiResponse.success(batchMetricsService.getStatisticsByJobType(jobType))
}
