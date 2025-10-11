package com.jigeumopen.jigeum.cafe

import com.jigeumopen.jigeum.cafe.dto.request.SearchCafeRequest
import com.jigeumopen.jigeum.cafe.service.CafeService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class CafeSearchIntegrationTest {

    @Autowired
    lateinit var searchService: CafeService

    @Test
    fun `should search nearby cafes`() = runBlocking {
        // Given
        val request = SearchCafeRequest(
            lat = 37.4979,
            lng = 127.0276,
            radius = 1000,
            time = "14:00",
            page = 0,
            size = 10
        )

        // When
        val result = searchService.searchNearby(request)

        // Then
        assertNotNull(result)
        assertTrue(result.content.isNotEmpty() || result.totalElements == 0L)
    }
}
