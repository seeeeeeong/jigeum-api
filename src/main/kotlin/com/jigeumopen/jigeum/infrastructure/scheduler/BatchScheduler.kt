package com.jigeumopen.jigeum.infrastructure.scheduler

import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobType
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import com.jigeumopen.jigeum.batch.service.DataProcessingService
import com.jigeumopen.jigeum.batch.service.RawDataCollectionService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "batch.scheduler",
    name = ["enabled"],
    havingValue = "true"
)
class BatchScheduler(
    private val rawDataCollectionService: RawDataCollectionService,
    private val dataProcessingService: DataProcessingService,
    private val batchJobRepository: BatchJobRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${batch.scheduler.collect-cron:0 0 3 L * *}")
    fun collectRawData() {
        logger.info("Starting scheduled raw data collection")

        if (batchJobRepository.hasRunningJob(JobType.COLLECT_RAW_DATA)) {
            logger.warn("Raw data collection is already running, skipping")
            return
        }

        runBlocking {
            try {
                val batchJob = rawDataCollectionService.collectRawData()
                logger.info("Scheduled raw data collection completed: {}", batchJob.batchId)
            } catch (e: Exception) {
                logger.error("Scheduled raw data collection failed", e)
            }
        }
    }

    @Scheduled(cron = "\${batch.scheduler.process-cron:0 0 5 L * *}")
    fun processRawData() {
        logger.info("Starting scheduled data processing")

        if (batchJobRepository.hasRunningJob(JobType.PROCESS_RAW_DATA)) {
            logger.warn("Data processing is already running, skipping")
            return
        }

        runBlocking {
            try {
                val batchJob = dataProcessingService.processRawData(reprocessAll = false)
                logger.info("Scheduled data processing completed: {}", batchJob.batchId)
            } catch (e: Exception) {
                logger.error("Scheduled data processing failed", e)
            }
        }
    }

    @Scheduled(cron = "\${batch.scheduler.retry-cron:0 0 4 L * *}")
    fun retryFailedProcessing() {
        logger.info("Starting retry for failed data processing")

        if (batchJobRepository.hasRunningJob(JobType.PROCESS_RAW_DATA)) {
            logger.warn("Data processing is already running, skipping retry")
            return
        }

        runBlocking {
            try {
                val resetCount = batchJobRepository.findAll()
                    .count { it.status == BatchJob.JobStatus.FAILED }

                if (resetCount > 0) {
                    logger.info("Retrying {} failed batch jobs", resetCount)
                    val batchJob = dataProcessingService.processRawData(reprocessAll = false)
                    logger.info("Retry processing completed: {}", batchJob.batchId)
                }
            } catch (e: Exception) {
                logger.error("Retry processing failed", e)
            }
        }
    }
}
