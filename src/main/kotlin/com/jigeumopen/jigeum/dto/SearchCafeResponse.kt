package com.jigeumopen.jigeum.dto

data class SearchCafeRequest(
    val lat: Double,
    val lng: Double,
    val radius: Int = 1000,
    val time: String,
    val page: Int = 0,
    val size: Int = 20
)
