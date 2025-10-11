package com.jigeumopen.jigeum.batch.controller

import com.jigeumopen.jigeum.batch.dto.BatchJobResponse
import com.jigeumopen.jigeum.batch.dto.BatchStatusResponse
import com.jigeumopen.jigeum.batch.dto.BatchStatusResponseDto
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import com.jigeumopen.jigeum.batch.repository.GooglePlacesRawDataRepository
import com.jigeumopen.jigeum.batch.service.DataProcessingService
import com.jigeumopen.jigeum.batch.service.RawDataCollectionService
import com.jigeumopen.jigeum.cafe.dto.response.ApiResponse
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/admin/batch")
class BatchController(
    private val rawDataCollectionService: RawDataCollectionService,
    private val dataProcessingService: DataProcessingService,
    private val batchJobRepository: BatchJobRepository,
    private val rawDataRepository: GooglePlacesRawDataRepository
) {

    @PostMapping("/collect")
    suspend fun collectRawData(): ApiResponse<BatchJobResponse> {
        val batchJob = rawDataCollectionService.collectRawData()
        return ApiResponse.success(BatchJobResponse.from(batchJob))
    }

    @PostMapping("/process")
    suspend fun processRawData(
        @RequestParam(defaultValue = "false") reprocessAll: Boolean
    ): ApiResponse<BatchJobResponse> {
        val batchJob = dataProcessingService.processRawData(reprocessAll)
        return ApiResponse.success(BatchJobResponse.from(batchJob))
    }

    @GetMapping("/status")
    fun getBatchStatus(): ApiResponse<BatchStatusResponseDto> {
        val totalRawData = rawDataRepository.count()
        val unprocessedCount = rawDataRepository.countByProcessed(false)

        val recentJobs = batchJobRepository.findTop10ByOrderByStartedAtDesc()
        val runningJobs = batchJobRepository.findByJobTypesAndStatus(
            listOf(BatchJob.JobType.COLLECT_RAW_DATA, BatchJob.JobType.PROCESS_RAW_DATA),
            BatchJob.JobStatus.RUNNING
        )

        val statusResponse = BatchStatusResponse(
            totalRawData = totalRawData,
            unprocessedData = unprocessedCount,
            runningJobs = runningJobs,
            recentJobs = recentJobs
        ).toResponse()

        return ApiResponse.success(statusResponse)
    }

    @GetMapping("/job/{batchId}")
    fun getBatchJob(@PathVariable batchId: String) =
        batchJobRepository.findByBatchId(batchId)?.let {
            ApiResponse.success(BatchJobResponse.from(it))
        } ?: throw IllegalArgumentException("Batch job not found: $batchId")

    @PostMapping("/cleanup")
    fun cleanupStuckJobs(): ApiResponse<String> {
        val timeout = LocalDateTime.now().minusHours(6)
        val stuckJobs = batchJobRepository.findStuckJobs(timeout)

        stuckJobs.forEach { job ->
            job.complete(
                status = BatchJob.JobStatus.FAILED,
                message = "Job timeout - marked as failed by cleanup"
            )
        }

        batchJobRepository.saveAll(stuckJobs)

        return ApiResponse.success("Cleaned up ${stuckJobs.size} stuck jobs")
    }
}
