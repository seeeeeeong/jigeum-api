package com.jigeumopen.jigeum.batch.repository

import com.jigeumopen.jigeum.batch.entity.BatchJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BatchJobRepository : JpaRepository<BatchJob, Long> {
    fun findByBatchId(batchId: String): BatchJob?

    fun findByStatusOrderByStartedAtDesc(
        status: BatchJob.JobStatus = BatchJob.JobStatus.RUNNING
    ): List<BatchJob>

    fun findByStatusAndStartedAtBefore(
        status: BatchJob.JobStatus = BatchJob.JobStatus.RUNNING,
        startedAt: LocalDateTime
    ): List<BatchJob>

}
