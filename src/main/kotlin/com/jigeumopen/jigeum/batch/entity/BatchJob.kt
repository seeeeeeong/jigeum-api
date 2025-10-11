package com.jigeumopen.jigeum.batch.entity

import com.jigeumopen.jigeum.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "batch_jobs")
class BatchJob(
    @Column(name = "batch_id", nullable = false, unique = true, length = 50)
    val batchId: String,
    
    @Column(name = "job_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val jobType: JobType,
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.RUNNING,
    
    @Column(name = "total_count")
    var totalCount: Int = 0,
    
    @Column(name = "processed_count")
    var processedCount: Int = 0,
    
    @Column(name = "success_count")
    var successCount: Int = 0,
    
    @Column(name = "error_count")
    var errorCount: Int = 0,
    
    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,
    
    @Column(length = 1000)
    var message: String? = null
) : BaseEntity() {
    
    enum class JobType {
        COLLECT_RAW_DATA,
        PROCESS_RAW_DATA
    }
    
    enum class JobStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        PARTIAL_SUCCESS
    }
    
    fun updateProgress(processed: Int, success: Int, error: Int) {
        this.processedCount = processed
        this.successCount = success
        this.errorCount = error
    }
    
    fun complete(status: JobStatus = JobStatus.COMPLETED, message: String? = null) {
        this.status = status
        this.completedAt = LocalDateTime.now()
        this.message = message
    }
}
