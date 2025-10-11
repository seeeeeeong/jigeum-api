package com.jigeumopen.jigeum.batch.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobStatus
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobType
import com.jigeumopen.jigeum.batch.entity.GooglePlacesRawData
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import com.jigeumopen.jigeum.batch.repository.GooglePlacesRawDataRepository
import com.jigeumopen.jigeum.cafe.client.GooglePlacesClient
import com.jigeumopen.jigeum.common.config.SeoulGridLocations
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class RawDataCollectionService(
    private val googleClient: GooglePlacesClient,
    private val rawDataRepository: GooglePlacesRawDataRepository,
    private val batchJobRepository: BatchJobRepository,
    private val gridLocations: SeoulGridLocations,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun collectRawData(): BatchJob = coroutineScope {
        val batchId = generateBatchId()
        val batchJob = createBatchJob(batchId)
        val semaphore = Semaphore(3)

        logger.info("Starting raw data collection batch: {}", batchId)

        try {
            val locations = gridLocations.getAll()
            val totalLocations = locations.size

            val results = locations.map { location ->
                async {
                    semaphore.withPermit {
                        delay(1000)
                        runCatching { collectLocationData(location, batchId) }
                            .onFailure { e -> logger.error("Failed to collect from ${location.name}", e) }
                            .getOrDefault(0)
                    }
                }
            }.awaitAll()

            val totalCafes = results.sum()
            val successCount = results.count { it > 0 }
            val errorCount = totalLocations - successCount

            val status = when {
                errorCount == 0 -> JobStatus.COMPLETED
                successCount == 0 -> JobStatus.FAILED
                else -> JobStatus.PARTIAL_SUCCESS
            }

            batchJob.updateProgress(
                processed = totalLocations,
                success = successCount,
                error = errorCount
            )
            batchJob.totalCount = totalCafes
            batchJob.complete(
                status = status,
                message = "Collected $totalCafes cafes from $successCount/$totalLocations locations"
            )

            batchJobRepository.save(batchJob)
            logger.info("Raw data collection completed: {}, Total: {}", batchId, totalCafes)

            batchJob
        } catch (e: Exception) {
            logger.error("Batch failed: {}", batchId, e)
            batchJob.complete(JobStatus.FAILED, e.message)
            batchJobRepository.save(batchJob)
            throw e
        }
    }

    private suspend fun collectLocationData(
        location: SeoulGridLocations.GridLocation,
        batchId: String
    ): Int {
        val response = googleClient.searchNearbyCafes(
            latitude = location.latitude,
            longitude = location.longitude,
            radius = 3000.0
        )

        val places = response.places.orEmpty()
        if (places.isEmpty()) return 0

        val placeIds = places.map { it.id }
        val existingIds = rawDataRepository.findExistingPlaceIds(placeIds)

        val newPlaces = places.filterNot { it.id in existingIds }
        if (newPlaces.isEmpty()) return 0

        val rawDataEntities = newPlaces.map { place ->
            GooglePlacesRawData.fromPlace(place, batchId, objectMapper)
        }

        rawDataRepository.saveAll(rawDataEntities)
        return newPlaces.size
    }

    @Transactional
    fun createBatchJob(batchId: String): BatchJob {
        return batchJobRepository.save(
            BatchJob(
                batchId = batchId,
                jobType = JobType.COLLECT_RAW_DATA,
                status = JobStatus.RUNNING
            )
        )
    }

    private fun generateBatchId(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val uuid = UUID.randomUUID().toString().take(8)
        return "RAW_${timestamp}_$uuid"
    }
}
