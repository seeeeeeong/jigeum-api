package com.jigeumopen.jigeum.domain.repository

import com.jigeumopen.jigeum.domain.entity.Cafe
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalTime

@Repository
interface CafeRepository : JpaRepository<Cafe, Long> {

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

    fun countByCloseTimeAfter(@Param("time") time: LocalTime): Long

}
