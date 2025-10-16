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
    var successCount: Int = 0,
    var errorCount: Int = 0
) {

}
