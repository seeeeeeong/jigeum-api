package com.jigeumopen.jigeum.batch.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "batch_jobs")
class BatchJob(
    @Id
    val batchId: String,

    @Enumerated(EnumType.STRING)
    val jobType: JobType,

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.RUNNING,

    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    var totalCount: Int = 0,
    var processedCount: Int = 0,
    var successCount: Int = 0,
    var errorCount: Int = 0
) {
    fun updateStatus(newStatus: JobStatus, total: Int? = null) {
        this.status = newStatus
        this.completedAt = LocalDateTime.now()
        total?.let { this.totalCount = it }
    }

    fun updateCount(processed: Int, success: Int, error: Int) {
        this.processedCount = processed
        this.successCount = success
        this.errorCount = error
    }
}
