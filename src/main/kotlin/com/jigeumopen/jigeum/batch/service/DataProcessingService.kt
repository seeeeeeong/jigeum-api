package com.jigeumopen.jigeum.batch.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jigeumopen.jigeum.batch.dto.BatchJobResponse
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.GooglePlacesRawData
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import com.jigeumopen.jigeum.batch.repository.GooglePlacesRawDataRepository
import com.jigeumopen.jigeum.cafe.entity.Cafe
import com.jigeumopen.jigeum.cafe.entity.CafeOperatingHour
import com.jigeumopen.jigeum.cafe.repository.CafeOperatingHourRepository
import com.jigeumopen.jigeum.cafe.repository.CafeRepository
import com.jigeumopen.jigeum.common.util.GeometryUtils
import com.jigeumopen.jigeum.infrastructure.client.dto.RegularOpeningHours
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
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

    companion object {
        private const val BATCH_SIZE = 100
    }

    suspend fun processRawData(reprocessAll: Boolean = false): BatchJobResponse = coroutineScope {
        val batchId = UUID.randomUUID().toString().take(8)
        val batchJob = batchJobRepository.save(
            BatchJob(batchId, BatchJob.JobType.PROCESS_RAW_DATA, BatchJob.JobStatus.RUNNING)
        )

        logger.info("Starting data processing batch: {}, reprocessAll: {}", batchId, reprocessAll)

        try {
            val totalCount = if (reprocessAll) rawDataRepository.count()
            else rawDataRepository.countByProcessed(false)
            val totalPages = ((totalCount + BATCH_SIZE - 1) / BATCH_SIZE).toInt()

            var processedCount = 0
            var successCount = 0
            var errorCount = 0

            for (page in 0 until totalPages) {
                val rawDataPage = if (reprocessAll)
                    rawDataRepository.findAll(PageRequest.of(page, BATCH_SIZE))
                else
                    rawDataRepository.findByProcessed(false, PageRequest.of(page, BATCH_SIZE))

                val results = rawDataPage.content.map { rawData ->
                    async(Dispatchers.IO) {
                        runCatching {
                            processRawDataItem(rawData)
                            true
                        }.getOrElse { e ->
                            logger.error("Failed to process raw data: {}", rawData.placeId, e)
                            false
                        }
                    }
                }.awaitAll()

                val pageSuccess = results.count { it }
                val pageError = results.count { !it }
                processedCount += rawDataPage.content.size
                successCount += pageSuccess
                errorCount += pageError

                batchJob.updateProgress(processedCount, successCount, errorCount)
                batchJobRepository.save(batchJob)

                logger.info(
                    "Processing progress: {}/{}, Success: {}, Error: {}",
                    processedCount, totalCount, successCount, errorCount
                )

                delay(100)
            }

            batchJob.completeWithResult(processedCount, successCount, errorCount)
            batchJobRepository.save(batchJob)
            logger.info("Data processing completed: {}, Success: {}/{}", batchId, successCount, processedCount)

            BatchJobResponse.from(batchJob)
        } catch (e: Exception) {
            logger.error("Batch processing failed: {}", batchId, e)
            batchJob.completeFailed()
            batchJobRepository.save(batchJob)
            throw e
        }
    }

    @Transactional
    fun processRawDataItem(rawData: GooglePlacesRawData) {
        val cafe = cafeRepository.findByPlaceId(rawData.placeId)
            ?: Cafe.create(rawData, geometryUtils.createPoint(rawData.longitude, rawData.latitude))
                .also { cafeRepository.save(it) }

        rawData.openingHours?.let { processOperatingHours(cafe, it) }
        rawData.markAsProcessed()
        rawDataRepository.save(rawData)
    }

    @Transactional
    fun processOperatingHours(cafe: Cafe, openingHoursJson: String) {
        try {
            operatingHourRepository.deleteByPlaceId(cafe.placeId)

            val openingHours = objectMapper.readValue<RegularOpeningHours>(openingHoursJson)
            val operatingHours = openingHours.periods
                ?.map { period ->
                    CafeOperatingHour.create(
                        cafe.placeId,
                        period.open.day,
                        LocalTime.of(period.open.hour, period.open.minute),
                        LocalTime.of(period.close.hour, period.close.minute)
                    )
                }
                .orEmpty()

            operatingHourRepository.saveAll(operatingHours)
        } catch (e: Exception) {
            logger.error("Failed to process operating hours for cafe: {}", cafe.placeId, e)
        }
    }
}
