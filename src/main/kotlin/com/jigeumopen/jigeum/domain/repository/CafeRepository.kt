package com.jigeumopen.jigeum.domain.repository

import com.jigeumopen.jigeum.domain.entity.Cafe
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalTime

@Repository
interface CafeRepository : JpaRepository<Cafe, Long> {

    @Query(value = """
        SELECT c.* FROM cafes c 
        WHERE ST_DWithin(
            c.location::geography, 
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, 
            :radius
        )
        AND c.close_time >= :requiredTime
        ORDER BY ST_Distance(
            c.location::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
        )
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    fun findNearbyOpenCafes(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radius") radius: Int,
        @Param("requiredTime") requiredTime: LocalTime,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<Cafe>

    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Cafe>

    fun findByCategory(category: String, pageable: Pageable): Page<Cafe>

    fun existsByName(name: String): Boolean

    @Query("SELECT COUNT(c) FROM Cafe c WHERE c.closeTime >= :time")
    fun countOpenCafesAt(@Param("time") time: LocalTime): Long

    @Query("SELECT AVG(c.rating) FROM Cafe c WHERE c.rating IS NOT NULL")
    fun getAverageRating(): Double?

    @Query("SELECT c.category, COUNT(c) FROM Cafe c GROUP BY c.category")
    fun getCategoryCounts(): List<Array<Any>>
}
