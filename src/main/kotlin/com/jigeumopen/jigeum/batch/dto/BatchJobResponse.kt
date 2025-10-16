// BatchJobResponse.kt
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

data class BatchStatusResponse(
    val totalRawData: Long,
    val processedCount: Long,
    val unprocessedData: Long,
    val runningJobs: List<BatchJobResponse>,
) {
    companion object {
        fun of(
            totalRawData: Long,
            processedCount: Long,
            runningJobs: List<BatchJob>,
        ): BatchStatusResponse {
            val unprocessedData = (totalRawData - processedCount).coerceAtLeast(0)
            return BatchStatusResponse(
                totalRawData = totalRawData,
                processedCount = processedCount,
                unprocessedData = unprocessedData,
                runningJobs = runningJobs.map { BatchJobResponse.from(it) }
            )
        }
    }
}
