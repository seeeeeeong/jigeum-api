package com.jigeumopen.jigeum.controller

import com.jigeumopen.jigeum.dto.CafeResponse
import com.jigeumopen.jigeum.dto.CreateCafeRequest
import com.jigeumopen.jigeum.service.CafeDataCollector
import com.jigeumopen.jigeum.service.CafeService
import com.jigeumopen.jigeum.service.SeoulGridCollector
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalTime

@RestController
@RequestMapping("/api/v1/cafes")
class CafeController(
    private val cafeService: CafeService,
    private val cafeDataCollector: CafeDataCollector,
    private val seoulGridCollector: SeoulGridCollector
) {

    @GetMapping
    fun getAllCafes(): ResponseEntity<List<CafeResponse>> =
        ResponseEntity.ok(cafeService.getAllCafes())

    @GetMapping("/{id}")
    fun getCafe(@PathVariable id: Long): ResponseEntity<CafeResponse> =
        ResponseEntity.ok(cafeService.getCafe(id))

    @GetMapping("/search")
    fun searchCafes(request: com.jigeumopen.jigeum.dto.SearchCafeRequest): ResponseEntity<List<CafeResponse>> =
        ResponseEntity.ok(
            cafeService.searchCafes(
                latitude = request.lat,
                longitude = request.lng,
                radius = request.radius,
                requiredTime = LocalTime.parse(request.time),
                page = request.page,
                size = request.size
            )
        )

    @PostMapping
    fun createCafe(@RequestBody request: CreateCafeRequest): ResponseEntity<CafeResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(cafeService.createCafe(request))

    @PostMapping("/collect")
    fun collectCafes(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
        @RequestParam(defaultValue = "5000") radius: Int
    ): ResponseEntity<Map<String, Any>> {
        val savedCount = cafeDataCollector.collectCafesInArea(lat, lng, radius)
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "savedCount" to savedCount,
                "message" to "카페 데이터 수집 완료"
            )
        )
    }

    @PostMapping("/collect-all")
    fun collectAllSeoulCafes(): ResponseEntity<Map<String, Any>> {
        val results = seoulGridCollector.collectAllSeoulCafes()
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "results" to results,
                "totalCount" to results.values.sum()
            )
        )
    }
}
