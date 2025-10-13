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
        // 강남/서초
        GridLocation("강남역", 37.4979, 127.0276),
        GridLocation("선릉", 37.5048, 127.0493),
        GridLocation("삼성", 37.5087, 127.0633),
        GridLocation("역삼", 37.5007, 127.0361),
        GridLocation("교대", 37.4933, 127.0143),
        GridLocation("서초", 37.4837, 127.0324),
        GridLocation("양재", 37.4686, 127.0348),
        GridLocation("도곡", 37.4908, 127.0553),
        GridLocation("대치", 37.4941, 127.0628),
        GridLocation("개포", 37.4893, 127.0669),

        // 송파/강동
        GridLocation("잠실", 37.5133, 127.1000),
        GridLocation("석촌", 37.5048, 127.1062),
        GridLocation("송파", 37.5145, 127.1058),
        GridLocation("가락", 37.4965, 127.1184),
        GridLocation("천호", 37.5387, 127.1238),
        GridLocation("강동", 37.5301, 127.1238),
        GridLocation("둔촌", 37.5273, 127.1362),
        GridLocation("올림픽공원", 37.5206, 127.1214),

        // 마포/서대문
        GridLocation("홍대", 37.5571, 126.9245),
        GridLocation("신촌", 37.5553, 126.9369),
        GridLocation("이대", 37.5597, 126.9465),
        GridLocation("아현", 37.5571, 126.9558),
        GridLocation("공덕", 37.5438, 126.9516),
        GridLocation("마포", 37.5397, 126.9456),
        GridLocation("연남", 37.5658, 126.9254),
        GridLocation("서교", 37.5527, 126.9183),

        // 종로/중구
        GridLocation("광화문", 37.5720, 126.9769),
        GridLocation("종로", 37.5704, 126.9851),
        GridLocation("명동", 37.5635, 126.9825),
        GridLocation("을지로", 37.5660, 126.9910),
        GridLocation("동대문", 37.5714, 127.0093),
        GridLocation("혜화", 37.5820, 127.0019),
        GridLocation("성균관", 37.5880, 126.9943),
        GridLocation("창경궁", 37.5790, 127.0050),

        // 용산/이태원
        GridLocation("이태원", 37.5345, 126.9945),
        GridLocation("한남", 37.5340, 127.0043),
        GridLocation("용산", 37.5298, 126.9648),
        GridLocation("삼각지", 37.5347, 126.9731),

        // 성동/광진
        GridLocation("왕십리", 37.5610, 127.0374),
        GridLocation("성수", 37.5444, 127.0557),
        GridLocation("건대", 37.5405, 127.0701),
        GridLocation("뚝섬", 37.5475, 127.0471),
        GridLocation("구의", 37.5371, 127.0856),
        GridLocation("자양", 37.5353, 127.0795),

        // 영등포/구로
        GridLocation("여의도", 37.5219, 126.9245),
        GridLocation("영등포", 37.5156, 126.9075),
        GridLocation("신도림", 37.5087, 126.8911),
        GridLocation("구로", 37.4954, 126.8876),
        GridLocation("대림", 37.4935, 126.8989),
        GridLocation("신길", 37.5045, 126.9141),

        // 관악/동작
        GridLocation("사당", 37.4765, 126.9816),
        GridLocation("신림", 37.4843, 126.9297),
        GridLocation("봉천", 37.4823, 126.9516),
        GridLocation("노량진", 37.5126, 126.9425),
        GridLocation("상도", 37.5028, 126.9480),

        // 강북
        GridLocation("성북", 37.5893, 127.0167),
        GridLocation("미아", 37.6276, 127.0258),
        GridLocation("수유", 37.6377, 127.0254),
        GridLocation("노원", 37.6542, 127.0568),
        GridLocation("상계", 37.6598, 127.0732)
    )

    fun getAll(): List<GridLocation> = locations

    fun getByRegion(regionName: String): List<GridLocation> {
        return when(regionName.lowercase()) {
            "gangnam" -> locations.take(10)
            "songpa" -> locations.slice(10..17)
            "mapo" -> locations.slice(18..25)
            "jongno" -> locations.slice(26..33)
            else -> locations
        }
    }

    fun getTotalCount(): Int = locations.size
}
