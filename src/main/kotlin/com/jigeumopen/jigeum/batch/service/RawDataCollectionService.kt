package com.jigeumopen.jigeum.batch.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.jigeumopen.jigeum.batch.constant.SeoulGridLocations
import com.jigeumopen.jigeum.batch.dto.OperationCountResponse
import com.jigeumopen.jigeum.batch.dto.OperationResponse
import com.jigeumopen.jigeum.batch.entity.BatchJob
import com.jigeumopen.jigeum.batch.entity.CafeRawData
import com.jigeumopen.jigeum.batch.entity.JobStatus
import com.jigeumopen.jigeum.batch.entity.JobType
import com.jigeumopen.jigeum.batch.repository.CafeRawDataRepository
import com.jigeumopen.jigeum.infrastructure.client.GooglePlacesClient
import com.jigeumopen.jigeum.infrastructure.client.dto.Place
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RawDataCollectionService(
    private val googlePlacesClient: GooglePlacesClient,
    private val seoulGridLocations: SeoulGridLocations,
    private val objectMapper: ObjectMapper,
    private val cafeRawDataRepository: CafeRawDataRepository,
    private val batchJobService: BatchJobService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CONCURRENT_REQUESTS_LIMIT = 3
        private const val REQUEST_DELAY_MS = 1000L
        private const val SEARCH_RADIUS_METERS = 3000.0
    }

    suspend fun collectRawData(): OperationResponse = coroutineScope {
        val batchJob = createBatchJob()

        logger.info("Raw data collection started - ID: {}", batchJob.batchId)

        try {
            val collectionResult = collectFromAllLocations(batchJob.batchId)
            updateBatchCount(batchJob, collectionResult)

            logger.info(
                "Raw data collection completed - ID: {}, New places: {}, Success: {}, Failures: {}",
                batchJob.batchId, collectionResult.processedCount, collectionResult.successCount, collectionResult.errorCount
            )

            OperationResponse.from(batchJob)
        } catch (e: Exception) {
            handleException(batchJob, e)
            throw e
        }
    }

    private suspend fun collectFromAllLocations(batchId: String): OperationCountResponse = coroutineScope {
        val requestSemaphore = Semaphore(CONCURRENT_REQUESTS_LIMIT)
        val gridLocations = seoulGridLocations.getAll()

        val locationResults = gridLocations.map { gridLocation ->
            async {
                requestSemaphore.withPermit {
                    delay(REQUEST_DELAY_MS)
                    collectFromLocation(gridLocation, batchId)
                }
            }
        }.awaitAll()

        val successfulResults = locationResults.mapNotNull { it.getOrNull() }
        val totalNewPlaces = successfulResults.sum()
        val successfulLocations = successfulResults.size
        val failedLocations = locationResults.count { it.isFailure }

        OperationCountResponse(totalNewPlaces, successfulLocations, failedLocations)
    }

    private suspend fun collectFromLocation(
        gridLocation: SeoulGridLocations.GridLocation,
        batchId: String
    ): Result<Int> = runCatching {
        val searchResponse = googlePlacesClient.searchNearbyCafes(gridLocation.latitude, gridLocation.longitude, SEARCH_RADIUS_METERS)

        val foundPlaces = searchResponse.places.orEmpty()
        val newPlacesCount = saveNewPlaces(foundPlaces, batchId)

        logger.info(
            "Location processed - {}: Found {} places, {} new",
            gridLocation.name, foundPlaces.size, newPlacesCount
        )
        newPlacesCount
    }.onFailure { exception ->
        logger.warn("Failed to collect from location: {} - {}", gridLocation.name, exception.message)
    }

    private fun saveNewPlaces(places: List<Place>, batchId: String): Int {
        val placeIds = places.map { it.id }
        val existingPlaceIds = cafeRawDataRepository.findExistingPlaceIds(placeIds)
        val newPlaces = places.filterNot { it.id in existingPlaceIds }

        val rawDataEntities = newPlaces.map { place ->
            CafeRawData.of(place, batchId, objectMapper)
        }

        cafeRawDataRepository.saveAll(rawDataEntities)
        return newPlaces.size
    }

    private fun createBatchJob(): BatchJob {
        return batchJobService.createBatchJob(JobType.PROCESS_RAW_DATA, JobStatus.RUNNING)
    }

    private fun updateBatchCount(
        batchJob: BatchJob,
        collectionResult: OperationCountResponse
    ) {
        batchJobService.updateBatchCount(
            batchJob,
            collectionResult.processedCount,
            collectionResult.successCount,
            collectionResult.errorCount
        )
    }

    private fun handleException(batchJob: BatchJob, e: Exception) {
        logger.error("Raw data collection failed - ID: {}", batchJob.batchId, e)
        batchJobService.updateBatchStatus(batchJob, JobStatus.FAILED)
    }
}
