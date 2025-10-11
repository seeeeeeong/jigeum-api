package com.jigeumopen.jigeum.batch.dto

import com.jigeumopen.jigeum.batch.entity.BatchJob
import java.time.Duration
import java.time.LocalDateTime

data class BatchJobResponse(
    val batchId: String,
    val jobType: String,
    val status: String,
    val totalCount: Int,
    val processedCount: Int,
    val successCount: Int,
    val errorCount: Int,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val duration: String?,
    val message: String?
) {
    companion object {
        fun from(batchJob: BatchJob): BatchJobResponse {
            val duration = formatDuration(batchJob.startedAt, batchJob.completedAt)

            return BatchJobResponse(
                batchId = batchJob.batchId,
                jobType = batchJob.jobType.name,
                status = batchJob.status.name,
                totalCount = batchJob.totalCount,
                processedCount = batchJob.processedCount,
                successCount = batchJob.successCount,
                errorCount = batchJob.errorCount,
                startedAt = batchJob.startedAt,
                completedAt = batchJob.completedAt,
                duration = duration,
                message = batchJob.message
            )
        }

        private fun formatDuration(startedAt: LocalDateTime, completedAt: LocalDateTime?): String? {
            return completedAt?.let {
                val duration = Duration.between(startedAt, it)
                String.format("%02d:%02d:%02d",
                    duration.toHours(),
                    duration.toMinutesPart(),
                    duration.toSecondsPart()
                )
            }
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
