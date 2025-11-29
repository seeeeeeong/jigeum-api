package com.jigeumopen.jigeum.cafe.repository

import com.jigeumopen.jigeum.cafe.entity.Cafe
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalTime

@Repository
interface CafeRepository : JpaRepository<Cafe, Long> {
    fun findByPlaceId(placeId: String): Cafe?

    @Query(
        value = """
            SELECT c.*
            FROM cafes c
            INNER JOIN cafe_operating_hours oh ON oh.place_id = c.place_id
            WHERE oh.day_of_week = :dayOfWeek
              AND oh.open_time <= :time
              AND oh.close_time > :time
              AND ST_DWithin(
                  CAST(c.location AS geography),
                  CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                  :radius
              )
            ORDER BY ST_Distance(
                CAST(c.location AS geography),
                CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
            )
            LIMIT :limit OFFSET :offset
        """,
        nativeQuery = true
    )
    fun findNearbyOpenCafes(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radius") radius: Int,
        @Param("dayOfWeek") dayOfWeek: Int,
        @Param("time") time: LocalTime,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<Cafe>

    @Query(
        value = """
            SELECT COUNT(DISTINCT c.id)
            FROM cafes c
            INNER JOIN cafe_operating_hours oh ON oh.place_id = c.place_id
            WHERE oh.day_of_week = :dayOfWeek
              AND oh.open_time <= :time
              AND oh.close_time > :time
              AND ST_DWithin(
                  CAST(c.location AS geography),
                  CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                  :radius
              )
        """,
        nativeQuery = true
    )
    fun countNearbyOpenCafes(
        @Param("latitude") latitude: Double,
        @Param("longitude") longitude: Double,
        @Param("radius") radius: Int,
        @Param("dayOfWeek") dayOfWeek: Int,
        @Param("time") time: LocalTime
    ): Long
}
