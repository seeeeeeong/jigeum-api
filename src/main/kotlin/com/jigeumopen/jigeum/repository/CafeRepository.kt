package com.jigeumopen.jigeum.repository

import com.jigeumopen.jigeum.domain.Cafe
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CafeRepository : JpaRepository<Cafe, Long>
