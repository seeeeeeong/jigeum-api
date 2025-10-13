package com.jigeumopen.jigeum.batch.entity

import jakarta.persistence.*

@Entity
@Table(name = "batch_jobs")
class BatchJob(
    @Id
    val batchId: String,

    @Enumerated(EnumType.STRING)
    val jobType: JobType,

    @Enumerated(EnumType.STRING)
    var status: JobStatus,

    var totalCount: Int = 0,
    var processedCount: Int = 0,
    var successCount: Int = 0,
    var errorCount: Int = 0
) {

    enum class JobType {
        COLLECT_RAW_DATA, PROCESS_RAW_DATA
    }

    enum class JobStatus {
        RUNNING, COMPLETED, FAILED, PARTIAL_SUCCESS;

        companion object {
            fun from(success: Int, error: Int): JobStatus = when {
                error == 0 -> COMPLETED
                success == 0 -> FAILED
                else -> PARTIAL_SUCCESS
            }
        }
    }

    fun updateProgress(total: Int, success: Int, error: Int) {
        this.processedCount = total
        this.successCount = success
        this.errorCount = error
    }

    fun completeWithResult(total: Int, success: Int, error: Int) {
        updateProgress(total, success, error)
        this.status = JobStatus.from(success, error)
        this.totalCount = total
    }

    fun completeFailed() {
        this.status = JobStatus.FAILED
    }
}
