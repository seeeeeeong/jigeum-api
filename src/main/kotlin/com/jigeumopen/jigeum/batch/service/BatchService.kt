package com.jigeumopen.jigeum.batch.service

import com.jigeumopen.jigeum.batch.dto.BatchJobResponse
import com.jigeumopen.jigeum.batch.dto.BatchStatusResponse
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.JobStatus
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import com.jigeumopen.jigeum.batch.repository.GooglePlacesRawDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BatchService(
    private val batchJobRepository: BatchJobRepository,
    private val rawDataRepository: GooglePlacesRawDataRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getBatchStatus(): BatchStatusResponse {
        val totalRawData = rawDataRepository.count()
        val processedCount = rawDataRepository.countByProcessed(true)
        val runningJobs = batchJobRepository.findByStatusOrderByStartedAtDesc(JobStatus.RUNNING)

        logger.info(
            "Fetched batch status: total={}, processed={}, unprocessed={}, running={}",
            totalRawData, processedCount, totalRawData - processedCount, runningJobs.size
        )

        return BatchStatusResponse.of(
            totalRawData = totalRawData,
            processedCount = processedCount,
            runningJobs = runningJobs,
        )
    }

    fun getBatchJob(batchId: String): BatchJobResponse {
        val job = batchJobRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("Batch job not found: $batchId")

        logger.info("Fetched batch job: {}", batchId)
        return BatchJobResponse.from(job)
    }

    fun cleanupStuckJobs(): String {
        val timeout = LocalDateTime.now().minusHours(6)
        val stuckJobs = batchJobRepository.findByStatusAndStartedAtBefore(
            status = BatchJob.JobStatus.RUNNING,
            startedAt = timeout
        )

        stuckJobs.forEach { job ->
            job.status = BatchJob.JobStatus.FAILED
            job.completedAt = LocalDateTime.now()
        }

        batchJobRepository.saveAll(stuckJobs)

        logger.info("Cleaned up {} stuck jobs older than {}", stuckJobs.size, timeout)
        return "Cleaned up ${stuckJobs.size} stuck jobs"
    }
}
