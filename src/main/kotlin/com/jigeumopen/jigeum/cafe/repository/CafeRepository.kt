package com.jigeumopen.jigeum.cafe.repository

import com.jigeumopen.jigeum.cafe.entity.Cafe
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalTime

@Repository
interface CafeRepository : JpaRepository<Cafe, Long> {

    fun existsByPlaceId(placeId: String): Boolean

    fun findByPlaceId(placeId: String): Cafe?

    @Query("SELECT c.placeId FROM Cafe c WHERE c.placeId IN :placeIds")
    fun findExistingPlaceIds(@Param("placeIds") placeIds: List<String>): Set<String>

    @Query(value = """
        SELECT c.* 
        FROM cafes c
        WHERE EXISTS (
            SELECT 1 
            FROM cafe_operating_hours oh
            WHERE oh.cafe_id = c.id
            AND oh.day_of_week = :dayOfWeek
            AND oh.open_time <= :time
            AND oh.close_time > :time
        )
        AND ST_DWithin(
            c.location::geography, 
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, 
            :radius
        )
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
        @Param("dayOfWeek") dayOfWeek: Int,
        @Param("time") time: LocalTime,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<Cafe>

    @Query(value = """
        SELECT COUNT(*) 
        FROM cafes c
        WHERE EXISTS (
            SELECT 1 
            FROM cafe_operating_hours oh
            WHERE oh.cafe_id = c.id
            AND oh.day_of_week = :dayOfWeek
            AND oh.open_time <= :time
            AND oh.close_time > :time
        )
        AND ST_DWithin(
            c.location::geography, 
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, 
            :radius
        )
    """, nativeQuery = true)
    fun countNearbyOpenCafes(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radius") radius: Int,
        @Param("dayOfWeek") dayOfWeek: Int,
        @Param("time") time: LocalTime
    ): Long
}
