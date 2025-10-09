package com.jigeumopen.jigeum.cafe.repository

import com.jigeumopen.jigeum.cafe.entity.CafeOperatingHour
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CafeOperatingHourRepository : JpaRepository<CafeOperatingHour, Long> {
    fun findByCafeId(cafeId: Long): List<CafeOperatingHour>
}
