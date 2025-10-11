package com.jigeumopen.jigeum.batch.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobStatus
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobType
import com.jigeumopen.jigeum.batch.entity.GooglePlacesRawData
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import com.jigeumopen.jigeum.batch.repository.GooglePlacesRawDataRepository
import com.jigeumopen.jigeum.cafe.dto.RegularOpeningHours
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.entity.CafeOperatingHour
import com.jigeumopen.jigeum.cafe.repository.CafeOperatingHourRepository
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.util.GeometryUtils
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class DataProcessingService(
    private val rawDataRepository: GooglePlacesRawDataRepository,
    private val cafeRepository: CafeRepository,
    private val operatingHourRepository: CafeOperatingHourRepository,
    private val batchJobRepository: BatchJobRepository,
    private val geometryUtils: GeometryUtils,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    companion object { private const val BATCH_SIZE = 100 }

    suspend fun processRawData(reprocessAll: Boolean = false): BatchJob = coroutineScope {
        val batchId = generateBatchId()
        val batchJob = createBatchJob(batchId)
        logger.info("Starting data processing batch: {}, reprocessAll: {}", batchId, reprocessAll)

        try {
            val totalCount = if (reprocessAll) rawDataRepository.count()
            else rawDataRepository.countByProcessed(false)
            val totalPages = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE

            var processedCount = 0
            var successCount = 0
            var errorCount = 0

            for (page in 0 until totalPages) {
                val rawDataPage = if (reprocessAll) rawDataRepository.findAll(PageRequest.of(page.toInt(), BATCH_SIZE))
                else rawDataRepository.findByProcessed(false, PageRequest.of(page.toInt(), BATCH_SIZE))

                val results = rawDataPage.content.map { rawData ->
                    async(Dispatchers.IO) {
                        runCatching { processRawDataItem(rawData); true }
                            .getOrElse { e ->
                                logger.error("Failed to process raw data: {}", rawData.placeId, e)
                                rawData.markAsError(e.message ?: "Unknown error")
                                rawDataRepository.save(rawData)
                                false
                            }
                    }
                }.awaitAll()

                processedCount += rawDataPage.content.size
                successCount += results.count { it }
                errorCount += results.count { !it }

                batchJob.updateProgress(processedCount, successCount, errorCount)
                batchJobRepository.save(batchJob)
                logger.info("Processing progress: {}/{}, Success: {}, Error: {}",
                    processedCount, totalCount, successCount, errorCount)
                delay(100)
            }

            val status = when {
                errorCount == 0 -> JobStatus.COMPLETED
                successCount == 0 -> JobStatus.FAILED
                else -> JobStatus.PARTIAL_SUCCESS
            }

            batchJob.totalCount = processedCount
            batchJob.complete(status, "Processed $successCount/$processedCount items successfully")
            batchJobRepository.save(batchJob)
            logger.info("Data processing completed: {}, Success: {}/{}", batchId, successCount, processedCount)
        } catch (e: Exception) {
            logger.error("Batch processing failed: {}", batchId, e)
            batchJob.complete(JobStatus.FAILED, e.message)
            batchJobRepository.save(batchJob)
            throw e
        }

        batchJob
    }

    @Transactional
    fun processRawDataItem(rawData: GooglePlacesRawData) {
        val cafe = cafeRepository.findByPlaceId(rawData.placeId) ?:
            Cafe.create(rawData, geometryUtils.createPoint(rawData.longitude, rawData.latitude))
                .also { cafeRepository.save(it) }

        cafeRepository.save(cafe)
        rawData.openingHours?.let { processOperatingHours(cafe.id!!, it) }

        rawData.markAsProcessed()
        rawDataRepository.save(rawData)
    }

    @Transactional
    fun processOperatingHours(cafeId: Long, openingHoursJson: String) {
        try {
            operatingHourRepository.deleteByCafeId(cafeId)

            val openingHours = objectMapper.readValue<RegularOpeningHours>(openingHoursJson)
            val operatingHours = openingHours.periods
                ?.mapNotNull { period ->
                    CafeOperatingHour.fromPeriod(cafeId, period)
                }.orEmpty()

            operatingHourRepository.saveAll(operatingHours)

        } catch (e: Exception) {
            logger.error("Failed to process operating hours for cafe: {}", cafeId, e)
        }
    }


    @Transactional
    fun createBatchJob(batchId: String): BatchJob =
        batchJobRepository.save(BatchJob(batchId, JobType.PROCESS_RAW_DATA, JobStatus.RUNNING))

    private fun generateBatchId(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val uuid = UUID.randomUUID().toString().take(8)
        return "PROC_${timestamp}_$uuid"
    }
}
