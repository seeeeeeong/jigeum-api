package com.jigeumopen.jigeum.cafe.dto.response

data class AreaCollectionResponse(
    val savedCount: Int,
    val location: String,
    val radius: Int
)

data class BatchCollectionResponse(
    val results: Map<String, Int>,
    val totalCount: Int,
    val locations: Int
)
