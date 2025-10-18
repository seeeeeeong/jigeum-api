package com.jigeumopen.jigeum.batch.service

import com.jigeumopen.jigeum.batch.dto.OperationResponse
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.JobStatus
import com.jigeumopen.jigeum.batch.entity.JobType
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class BatchJobService(
    private val batchJobRepository: BatchJobRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val STUCK_JOB_TIMEOUT_HOURS = 6L
    }

    fun getBatchJob(batchId: String): OperationResponse {
        val job = batchJobRepository.findByBatchId(batchId)
            ?: throw IllegalArgumentException("Batch job not found with ID: $batchId")

        return OperationResponse.from(job)
    }

    @Transactional
    fun cleanupStuckJobs(): String {
        val cutoffTime = LocalDateTime.now().minusHours(STUCK_JOB_TIMEOUT_HOURS)
        val stuckJobs = batchJobRepository.findByStatusAndStartedAtBefore(JobStatus.RUNNING, cutoffTime)

        stuckJobs.forEach { job ->
            job.updateStatus(JobStatus.FAILED)
            batchJobRepository.save(job)
        }

        logger.warn(
            "Cleaned up {} stuck jobs that started before {}",
            stuckJobs.size, cutoffTime
        )
        return "Cleaned up ${stuckJobs.size} stuck job(s)"
    }

    fun createBatchJob(jobType: JobType, jobStatus: JobStatus): BatchJob {
        val batchId = UUID.randomUUID().toString().take(8)
        return batchJobRepository.save(BatchJob(batchId, jobType, jobStatus))
    }

    fun updateBatchCount(batchJob: BatchJob, processed: Int, success: Int, error: Int) {
        batchJob.updateCount(processed, success, error)
        batchJobRepository.save(batchJob)
    }

    fun updateBatchStatus(batchJob: BatchJob, jobStatus: JobStatus) {
        batchJob.updateStatus(jobStatus)
        batchJobRepository.save(batchJob)
    }
}
