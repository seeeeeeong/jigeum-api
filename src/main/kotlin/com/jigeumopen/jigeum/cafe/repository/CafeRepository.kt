package com.jigeumopen.jigeum.cafe.repository

import com.jigeumopen.jigeum.cafe.entity.Cafe
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

    @Query("""
        SELECT c 
        FROM Cafe c
        JOIN c.operatingHours oh
        WHERE oh.dayOfWeek = :dayOfWeek
        AND oh.openTime <= :time
        AND oh.closeTime > :time
        AND function('ST_DWithin', c.location, 
            function('ST_SetSRID', function('ST_MakePoint', :longitude, :latitude), 4326), 
            :radius) = true
        ORDER BY function('ST_Distance', c.location,
            function('ST_SetSRID', function('ST_MakePoint', :longitude, :latitude), 4326))
    """)
    fun findNearbyOpenCafes(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radius") radius: Int,
        @Param("dayOfWeek") dayOfWeek: Int,
        @Param("time") time: LocalTime
    ): List<Cafe>
}
