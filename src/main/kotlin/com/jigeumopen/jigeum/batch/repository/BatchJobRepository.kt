package com.jigeumopen.jigeum.batch.repository

import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobStatus
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BatchJobRepository : JpaRepository<BatchJob, Long> {

    fun findByBatchId(batchId: String): BatchJob?

    @Query("""
        SELECT b FROM BatchJob b 
        WHERE b.jobType IN :jobTypes
        AND b.status = :status
        ORDER BY b.startedAt DESC
    """)
    fun findByJobTypesAndStatus(
        @Param("jobTypes") jobTypes: List<JobType>,
        @Param("status") status: JobStatus
    ): List<BatchJob>

    fun findTop10ByOrderByStartedAtDesc(): List<BatchJob>

    @Query("""
        SELECT b FROM BatchJob b
        WHERE b.status = 'RUNNING'
        AND b.startedAt < :timeout
    """)
    fun findStuckJobs(@Param("timeout") timeout: LocalDateTime): List<BatchJob>

    @Query("""
        SELECT COUNT(b) > 0 FROM BatchJob b
        WHERE b.jobType = :jobType
        AND b.status = 'RUNNING'
    """)
    fun hasRunningJob(@Param("jobType") jobType: JobType): Boolean
}
