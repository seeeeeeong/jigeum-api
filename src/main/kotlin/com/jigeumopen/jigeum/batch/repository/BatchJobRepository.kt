package com.jigeumopen.jigeum.batch.repository

import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.JobStatus
import com.jigeumopen.jigeum.batch.entity.JobType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BatchJobRepository : JpaRepository<BatchJob, String> {
    fun findByBatchId(batchId: String): BatchJob?

    fun findByStatusAndStartedAtBefore(
        status: JobStatus,
        startedAt: LocalDateTime
    ): List<BatchJob>

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM BatchJob b
        WHERE b.jobType = :jobType AND b.status = :status
    """)
    fun hasRunningJob(
        @Param("jobType") jobType: JobType,
        @Param("status") status: JobStatus = JobStatus.RUNNING
    ): Boolean
}
