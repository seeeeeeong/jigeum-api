package com.jigeumopen.jigeum.batch.service

import com.jigeumopen.jigeum.batch.dto.OperationCountResponse
import com.jigeumopen.jigeum.batch.dto.OperationResponse
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.CafeRawData
import com.jigeumopen.jigeum.batch.entity.JobStatus
import com.jigeumopen.jigeum.batch.entity.JobType
import com.jigeumopen.jigeum.batch.repository.CafeRawDataRepository
import com.jigeumopen.jigeum.cafe.service.CafeService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class DataProcessingService(
    private val rawPlacesRepository: CafeRawDataRepository,
    private val batchJobService: BatchJobService,
    private val cafeService: CafeService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 100
        private const val BATCH_DELAY_MS = 100L
    }

    suspend fun processRawData(reprocessAll: Boolean = false): OperationResponse = coroutineScope {
        val batchJob = createBatchJob()

        logger.info("Processing batch started - ID: {}, Reprocess all: {}", batchJob.batchId, reprocessAll)

        try {
            val totalPlaces = getTotalPlaces(reprocessAll)
            val totalPages = ((totalPlaces + BATCH_SIZE - 1) / BATCH_SIZE).toInt()

            val processResult = processPages(batchJob, totalPages, totalPlaces, reprocessAll)
            updateProcessCount(batchJob, processResult.processedCount, processResult.successCount, processResult.errorCount)

            logger.info(
                "Processing batch completed - ID: {}, Success: {}/{}, Errors: {}",
                batchJob.batchId, processResult.successCount, processResult.processedCount, processResult.errorCount
            )

            OperationResponse.from(batchJob)
        } catch (e: Exception) {
            handleException(batchJob, e)
            throw e
        }
    }

    private suspend fun processPages(
        batchJob: BatchJob,
        totalPages: Int,
        totalPlaces: Long,
        reprocessAll: Boolean
    ): OperationCountResponse {
        var processedTotal = 0
        var successTotal = 0
        var errorTotal = 0

        val processedFilter: Boolean? = if (reprocessAll) null else false

        for (pageNumber in 0 until totalPages) {
            val pageData = getPageData(processedFilter, pageNumber)
            val pageResult = processPage(pageData)

            processedTotal += pageData.content.size
            successTotal += pageResult.successCount
            errorTotal += pageResult.errorCount

            updateProcessCount(batchJob, processedTotal, successTotal, errorTotal)

            logger.info(
                "Processing progress - Page: {}/{}, Records: {}/{}, Success: {}, Errors: {}",
                pageNumber + 1, totalPages, processedTotal, totalPlaces, successTotal, errorTotal
            )

            delay(BATCH_DELAY_MS)
        }

        return OperationCountResponse(processedTotal, successTotal, errorTotal)
    }

    private suspend fun processPage(pageData: org.springframework.data.domain.Page<CafeRawData>): OperationCountResponse = coroutineScope {
        val results = pageData.content.map { rawPlace ->
            async(Dispatchers.IO) {
                runCatching {
                    processRawDataToCafe(rawPlace)
                    true
                }.onFailure { e ->
                    logger.error("Failed to process place: {} - {}", rawPlace.placeId, e.message)
                }.getOrDefault(false)
            }
        }.awaitAll()

        val success = results.count { it }
        val error = results.size - success
        OperationCountResponse(results.size, success, error)
    }

    fun createBatchJob(): BatchJob {
        return batchJobService.createBatchJob(JobType.PROCESS_RAW_DATA, JobStatus.RUNNING)
    }

    private fun getPageData(
        processedFilter: Boolean?,
        pageNumber: Int
    ): Page<CafeRawData> {
        return rawPlacesRepository.findByProcessedNullable(
            processedFilter,
            PageRequest.of(pageNumber, BATCH_SIZE)
        )
    }

    fun getTotalPlaces(reprocessAll: Boolean): Long {
        return if (reprocessAll) rawPlacesRepository.count()
        else rawPlacesRepository.countByProcessed(false)
    }

    private fun processRawDataToCafe(rawPlace: CafeRawData) {
        cafeService.processRawDataToCafe(rawPlace)
    }

    fun updateProcessCount(batchJob: BatchJob, processed: Int, success: Int, error: Int) {
        batchJobService.updateBatchCount(batchJob, processed, success, error)
    }

    fun handleException(batchJob: BatchJob, e: java.lang.Exception) {
        logger.error("Batch processing failed - ID: {}", batchJob.batchId, e)
        batchJobService.updateBatchStatus(batchJob, JobStatus.FAILED)
    }

}
