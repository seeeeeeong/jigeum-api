package com.jigeumopen.jigeum.dto

import java.math.BigDecimal
import java.time.LocalTime

data class CreateCafeRequest(
    val name: String,
    val address: String?,
    val phone: String?,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val openTime: LocalTime?,
    val closeTime: LocalTime,
    val category: String?,
    val rating: BigDecimal?
)
