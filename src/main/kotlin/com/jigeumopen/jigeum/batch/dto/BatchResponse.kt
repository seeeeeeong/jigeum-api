package com.jigeumopen.jigeum.batch.dto

import com.jigeumopen.jigeum.batch.entity.BatchJob
import java.time.LocalDateTime

data class OperationResponse(
    val batchId: String,
    val jobType: String,
    val status: String,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val totalCount: Int,
    val processedCount: Int,
    val successCount: Int,
    val errorCount: Int,
) {
    companion object {
        fun from(batchJob: BatchJob): OperationResponse {
            return OperationResponse(
                batchId = batchJob.batchId,
                jobType = batchJob.jobType.name,
                status = batchJob.status.name,
                startedAt = batchJob.startedAt,
                completedAt = batchJob.completedAt,
                totalCount = batchJob.totalCount,
                processedCount = batchJob.processedCount,
                successCount = batchJob.successCount,
                errorCount = batchJob.errorCount
            )
        }
    }
}
data class OperationCountResponse(
    val processedCount: Int,
    val successCount: Int,
    val errorCount: Int
)
