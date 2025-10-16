package com.jigeumopen.jigeum.batch.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.jigeumopen.jigeum.batch.dto.BatchJobResponse
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobStatus
import com.jigeumopen.jigeum.batch.entity.BatchJob.JobType
import com.jigeumopen.jigeum.batch.entity.GooglePlacesRawData
import com.jigeumopen.jigeum.batch.repository.BatchJobRepository
import com.jigeumopen.jigeum.batch.repository.GooglePlacesRawDataRepository
import com.jigeumopen.jigeum.common.config.SeoulGridLocations
import com.jigeumopen.jigeum.infrastructure.client.GooglePlacesClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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

    suspend fun collectRawData(): BatchJobResponse = coroutineScope {
        val batchId = UUID.randomUUID().toString().take(8)
        val batchJob = batchJobRepository.save(BatchJob(batchId, JobType.COLLECT_RAW_DATA, JobStatus.RUNNING))

        val semaphore = Semaphore(3)
        logger.info("Starting raw data collection batch: {}", batchId)

        try {
            val locations = gridLocations.getAll()

            val results = locations.map { location ->
                async {
                    semaphore.withPermit {
                        delay(1000)
                        runCatching {
                            collectLocationData(location, batchId)
                        }.onSuccess { count ->
                            logger.info("Collected {} new cafes from {}", count, location.name)
                        }.onFailure { e ->
                            logger.warn("Failed to collect from ${location.name}: {}", e.message)
                        }
                    }
                }
            }.awaitAll()

            val successValues = results.mapNotNull { it.getOrNull() }
            val totalCafes = successValues.sum()
            val successCount = successValues.count()
            val errorCount = results.count { it.isFailure }

            batchJob.completeWithResult(totalCafes, successCount, errorCount)
            batchJobRepository.save(batchJob)

            logger.info("Raw data collection completed: {}, Total new cafes: {}", batchId, totalCafes)
            BatchJobResponse.from(batchJob)
        } catch (e: Exception) {
            logger.error("Batch failed: {}", batchId, e)
            batchJob.completeFailed()
            batchJobRepository.save(batchJob)
            throw e
        }
    }

    private suspend fun collectLocationData(
        location: SeoulGridLocations.GridLocation,
        batchId: String
    ): Int {
        val response = googleClient.searchNearbyCafes(location.latitude, location.longitude, 3000.0)
        val places = response.places.orEmpty()

        val existingIds = rawDataRepository.findExistingPlaceIds(places.map { it.id })
        val newPlaces = places.filterNot { it.id in existingIds }

        rawDataRepository.saveAll(
            newPlaces.map { GooglePlacesRawData.fromPlace(it, batchId, objectMapper) }
        )

        return newPlaces.size
    }

}
