package com.jigeumopen.jigeum.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SeoulGridCollector(
    private val cafeSearchService: CafeSearchService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun collectAllSeoulCafes(): Map<String, Int> {
        logger.info("===== 서울 전역 카페 데이터 수집 시작 =====")

        val results = mutableMapOf<String, Int>()
        var totalSaved = 0

        seoulLocations.forEach { (name, lat, lng) ->
            logger.info("[$name] 데이터 수집 시작...")
            Thread.sleep(2000)

            val count = cafeSearchService.collectCafesInArea(lat, lng, 3000)
            results[name] = count
            totalSaved += count

            logger.info("[$name] 완료: $count 개 저장")
        }

        logger.info("===== 전체 수집 완료: 총 $totalSaved 개 저장 =====")
        return results
    }

    companion object {
        private val seoulLocations = listOf(
            // 강남구
            Triple("강남역", 37.4979, 127.0276),
            Triple("선릉역", 37.5048, 127.0493),
            Triple("삼성역", 37.5087, 127.0633),
            Triple("역삼역", 37.5007, 127.0361),

            // 서초구
            Triple("교대역", 37.4933, 127.0143),
            Triple("강남대로", 37.4905, 127.0177),

            // 송파구
            Triple("잠실역", 37.5133, 127.1000),
            Triple("석촌호수", 37.5105, 127.0988),

            // 마포구
            Triple("홍대입구역", 37.5571, 126.9245),
            Triple("합정역", 37.5496, 126.9139),
            Triple("상수역", 37.5478, 126.9227),

            // 용산구
            Triple("이태원역", 37.5345, 126.9945),
            Triple("한남동", 37.5341, 127.0021),

            // 종로구
            Triple("광화문", 37.5720, 126.9769),
            Triple("경복궁역", 37.5757, 126.9732),
            Triple("안국역", 37.5760, 126.9852),

            // 중구
            Triple("시청역", 37.5665, 126.9780),
            Triple("명동역", 37.5635, 126.9825),
            Triple("을지로입구", 37.5660, 126.9824),

            // 성동구
            Triple("왕십리역", 37.5610, 127.0374),
            Triple("성수역", 37.5444, 127.0557),

            // 광진구
            Triple("건대입구역", 37.5405, 127.0701),

            // 동대문구
            Triple("회기역", 37.5895, 127.0571),

            // 성북구
            Triple("한성대입구", 37.5887, 127.0062),

            // 강북구
            Triple("수유역", 37.6383, 127.0254),

            // 은평구
            Triple("연신내역", 37.6192, 126.9211),

            // 서대문구
            Triple("신촌역", 37.5553, 126.9369),
            Triple("이대역", 37.5566, 126.9458),

            // 영등포구
            Triple("여의도역", 37.5219, 126.9245),
            Triple("영등포구청역", 37.5244, 126.8960),

            // 동작구
            Triple("사당역", 37.4765, 126.9816),
            Triple("이수역", 37.4866, 126.9820),

            // 관악구
            Triple("신림역", 37.4842, 126.9297),
            Triple("서울대입구역", 37.4812, 126.9527),

            // 강서구
            Triple("김포공항역", 37.5623, 126.8012),
            Triple("발산역", 37.5588, 126.8375)
        )
    }
}
