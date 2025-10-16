package com.jigeumopen.jigeum.batch.controller

import com.jigeumopen.jigeum.batch.dto.BatchJobResponse
import com.jigeumopen.jigeum.batch.dto.BatchStatusResponse
import com.jigeumopen.jigeum.batch.service.BatchService
import com.jigeumopen.jigeum.batch.service.DataProcessingService
import com.jigeumopen.jigeum.batch.service.RawDataCollectionService
import com.jigeumopen.jigeum.cafe.dto.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/batch")
class BatchController(
    private val rawDataCollectionService: RawDataCollectionService,
    private val dataProcessingService: DataProcessingService,
    private val batchService: BatchService
) {

    @PostMapping("/collect")
    suspend fun collectRawData(): ApiResponse<BatchJobResponse> =
        ApiResponse.success(rawDataCollectionService.collectRawData())

    @PostMapping("/process")
    suspend fun processRawData(
        @RequestParam(defaultValue = "false") reprocessAll: Boolean
    ): ApiResponse<BatchJobResponse> =
        ApiResponse.success(dataProcessingService.processRawData(reprocessAll))

    @GetMapping("/status")
    fun getBatchStatus(): ApiResponse<BatchStatusResponse> =
        ApiResponse.success(batchService.getBatchStatus())

    @GetMapping("/job/{batchId}")
    fun getBatchJob(@PathVariable batchId: String): ApiResponse<BatchJobResponse> =
        ApiResponse.success(batchService.getBatchJob(batchId))

    @PostMapping("/cleanup")
    fun cleanupStuckJobs(): ApiResponse<String> =
        ApiResponse.success(batchService.cleanupStuckJobs())
}
