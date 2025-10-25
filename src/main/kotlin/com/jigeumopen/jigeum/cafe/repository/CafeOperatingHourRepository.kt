package com.jigeumopen.jigeum.cafe.repository

import com.jigeumopen.jigeum.cafe.entity.CafeOperatingHour
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CafeOperatingHourRepository : JpaRepository<CafeOperatingHour, Long> {

    @Modifying
    @Query("DELETE FROM CafeOperatingHour oh WHERE oh.placeId = :placeId")
    fun deleteByPlaceId(@Param("placeId") placeId: String)

    fun findByPlaceIdOrderByDayOfWeekAsc(placeId: String): List<CafeOperatingHour>

}
