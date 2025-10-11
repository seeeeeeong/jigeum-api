package com.jigeumopen.jigeum.batch.repository

import com.jigeumopen.jigeum.batch.entity.GooglePlacesRawData
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GooglePlacesRawDataRepository : JpaRepository<GooglePlacesRawData, Long> {

    fun findByProcessed(processed: Boolean, pageable: Pageable): Page<GooglePlacesRawData>

    fun countByProcessed(processed: Boolean): Long

    @Query("SELECT r.placeId FROM GooglePlacesRawData r WHERE r.placeId IN :placeIds")
    fun findExistingPlaceIds(@Param("placeIds") placeIds: List<String>): Set<String>

}
