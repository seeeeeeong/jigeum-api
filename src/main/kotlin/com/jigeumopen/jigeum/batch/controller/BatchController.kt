package com.jigeumopen.jigeum.batch.controller

import com.jigeumopen.jigeum.batch.dto.OperationResponse
import com.jigeumopen.jigeum.batch.service.BatchJobService
import com.jigeumopen.jigeum.batch.service.DataProcessingService
import com.jigeumopen.jigeum.batch.service.RawDataCollectionService
import com.jigeumopen.jigeum.common.dto.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/batch")
class BatchController(
    private val rawDataCollectionService: RawDataCollectionService,
    private val dataProcessingService: DataProcessingService,
    private val batchJobService: BatchJobService
) {

    @PostMapping("/collect")
    suspend fun collectRawData(): ApiResponse<OperationResponse> =
        ApiResponse.success(rawDataCollectionService.collectRawData())

    @PostMapping("/process")
    suspend fun processRawData(
        @RequestParam(defaultValue = "false") reprocessAll: Boolean
    ): ApiResponse<OperationResponse> =
        ApiResponse.success(dataProcessingService.processRawData(reprocessAll))

    @GetMapping("/jobs/{batchId}")
    fun getBatchJob(@PathVariable batchId: String): ApiResponse<OperationResponse> =
        ApiResponse.success(batchJobService.getBatchJob(batchId))

    @PostMapping("/cleanup")
    fun cleanupStuckJobs(): ApiResponse<String> =
        ApiResponse.success(batchJobService.cleanupStuckJobs())
}
