package com.jigeumopen.jigeum.common.config

import org.springframework.stereotype.Component

@Component
class SeoulGridLocations {

    data class GridLocation(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    private val locations = listOf(
        GridLocation("강남역", 37.4979, 127.0276),
        GridLocation("선릉역", 37.5048, 127.0493),
        GridLocation("삼성역", 37.5087, 127.0633),
        GridLocation("역삼역", 37.5007, 127.0361),
        GridLocation("교대역", 37.4933, 127.0143),
        GridLocation("잠실역", 37.5133, 127.1000),
        GridLocation("홍대입구역", 37.5571, 126.9245),
        GridLocation("이태원역", 37.5345, 126.9945),
        GridLocation("광화문", 37.5720, 126.9769),
        GridLocation("명동역", 37.5635, 126.9825),
        GridLocation("왕십리역", 37.5610, 127.0374),
        GridLocation("성수역", 37.5444, 127.0557),
        GridLocation("건대입구역", 37.5405, 127.0701),
        GridLocation("신촌역", 37.5553, 126.9369),
        GridLocation("여의도역", 37.5219, 126.9245),
        GridLocation("사당역", 37.4765, 126.9816)
    )

    fun getAll(): List<GridLocation> = locations

    fun findByName(name: String): GridLocation? =
        locations.find { it.name == name }
}
