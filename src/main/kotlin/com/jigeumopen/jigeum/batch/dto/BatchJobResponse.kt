package com.jigeumopen.jigeum.batch.dto

import com.jigeumopen.jigeum.batch.entity.BatchJob

data class BatchJobResponse(
    val batchId: String,
    val jobType: String,
    val status: String,
    val totalCount: Int,
    val processedCount: Int,
    val successCount: Int,
    val errorCount: Int
) {
    companion object {
        fun from(batchJob: BatchJob): BatchJobResponse {
            return BatchJobResponse(
                batchId = batchJob.batchId,
                jobType = batchJob.jobType.name,
                status = batchJob.status.name,
                totalCount = batchJob.totalCount,
                processedCount = batchJob.processedCount,
                successCount = batchJob.successCount,
                errorCount = batchJob.errorCount
            )
        }
    }
}

data class BatchStatusResponseDto(
    val totalRawData: Long,
    val unprocessedData: Long,
    val processedData: Long,
    val runningJobs: List<BatchJobResponse>,
    val recentJobs: List<BatchJobResponse>
)

data class BatchStatusResponse(
    val totalRawData: Long,
    val unprocessedData: Long,
    val runningJobs: List<BatchJob>,
    val recentJobs: List<BatchJob>
) {
    private val processedData: Long get() = totalRawData - unprocessedData

    fun toResponse(): BatchStatusResponseDto {
        return BatchStatusResponseDto(
            totalRawData = totalRawData,
            unprocessedData = unprocessedData,
            processedData = processedData,
            runningJobs = runningJobs.map { BatchJobResponse.from(it) },
            recentJobs = recentJobs.map { BatchJobResponse.from(it) }
        )
    }
}
