package com.jigeumopen.jigeum.batch.service

import com.jigeumopen.jigeum.batch.dto.BatchStatistics
import com.jigeumopen.jigeum.batch.dto.JobTypeStatistics
import com.jigeumopen.jigeum.batch.entity.JobStatus
import com.jigeumopen.jigeum.batch.entity.JobType
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class BatchMetricsService(
    private val batchJobRepository: BatchJobRepository,
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 1분마다 배치 작업 메트릭 업데이트
     */
    @Scheduled(fixedRate = 60000)
    fun updateBatchMetrics() {
        try {
            val runningJobs = batchJobRepository.countByStatus(JobStatus.RUNNING)
            val failedJobs = batchJobRepository.countByStatus(JobStatus.FAILED)

            meterRegistry.gauge("batch.jobs.running", runningJobs)
            meterRegistry.gauge("batch.jobs.failed", failedJobs)

            logger.debug("Updated batch metrics - Running: {}, Failed: {}", runningJobs, failedJobs)
        } catch (e: Exception) {
            logger.error("Failed to update batch metrics", e)
        }
    }

    /**
     * 최근 24시간 배치 작업 통계 조회
     */
    fun getBatchStatistics(): BatchStatistics {
        val last24Hours = LocalDateTime.now().minusHours(24)
        val recentJobs = batchJobRepository.findAll().filter { it.startedAt.isAfter(last24Hours) }

        if (recentJobs.isEmpty()) {
            return BatchStatistics(
                totalJobs = 0,
                completedJobs = 0,
                failedJobs = 0,
                runningJobs = 0,
                successRate = 0.0,
                averageDurationMs = 0.0,
                errorRate = 0.0
            )
        }

        val completed = recentJobs.count { it.status == JobStatus.COMPLETED }
        val failed = recentJobs.count { it.status == JobStatus.FAILED }
        val running = recentJobs.count { it.status == JobStatus.RUNNING }

        val durations = recentJobs.mapNotNull { job ->
            job.completedAt?.let { completed ->
                Duration.between(job.startedAt, completed).toMillis()
            }
        }

        return BatchStatistics(
            totalJobs = recentJobs.size,
            completedJobs = completed,
            failedJobs = failed,
            runningJobs = running,
            successRate = if (recentJobs.size > 0) completed.toDouble() / recentJobs.size else 0.0,
            averageDurationMs = if (durations.isNotEmpty()) durations.average() else 0.0,
            errorRate = if (recentJobs.size > 0) failed.toDouble() / recentJobs.size else 0.0
        )
    }

    /**
     * 작업 타입별 통계
     */
    fun getStatisticsByJobType(jobType: JobType): JobTypeStatistics {
        val last24Hours = LocalDateTime.now().minusHours(24)
        val jobs = batchJobRepository.findAll()
            .filter { it.jobType == jobType && it.startedAt.isAfter(last24Hours) }

        val totalProcessed = jobs.sumOf { it.processedCount }
        val totalSuccess = jobs.sumOf { it.successCount }
        val totalErrors = jobs.sumOf { it.errorCount }

        return JobTypeStatistics(
            jobType = jobType.name,
            totalJobs = jobs.size,
            totalProcessed = totalProcessed,
            totalSuccess = totalSuccess,
            totalErrors = totalErrors,
            successRate = if (totalProcessed > 0) totalSuccess.toDouble() / totalProcessed else 0.0
        )
    }
}
