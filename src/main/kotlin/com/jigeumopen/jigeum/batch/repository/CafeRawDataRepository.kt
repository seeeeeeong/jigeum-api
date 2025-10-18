package com.jigeumopen.jigeum.batch.repository

import com.jigeumopen.jigeum.batch.entity.CafeRawData
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CafeRawDataRepository : JpaRepository<CafeRawData, Long> {

    @Query(
        """
        SELECT r FROM CafeRawData r
        WHERE (:processed IS NULL OR r.processed = :processed)
    """
    )
    fun findByProcessedNullable(
        @Param("processed") processed: Boolean?,
        pageable: Pageable
    ): Page<CafeRawData>

    fun countByProcessed(processed: Boolean): Long

    @Query(
        """
        SELECT r.placeId 
        FROM CafeRawData r 
        WHERE r.placeId IN :placeIds
    """
    )
    fun findExistingPlaceIds(@Param("placeIds") placeIds: List<String>): Set<String>
}
