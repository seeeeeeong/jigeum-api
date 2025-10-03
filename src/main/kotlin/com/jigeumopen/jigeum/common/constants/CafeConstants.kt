package com.jigeumopen.jigeum.common.constants

object CafeConstants {

    object Search {
        const val MIN_RADIUS_METERS = 100
        const val MAX_RADIUS_METERS = 50000
        const val DEFAULT_RADIUS_METERS = 1000

        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 100
        const val DEFAULT_PAGE_SIZE = 20
        const val DEFAULT_PAGE_NUMBER = 0
    }

    object Location {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
        const val SRID = 4326  // Spatial Reference ID
    }

    object Api {
        const val BASE_PATH = "/api/v1"
        const val SLOW_API_THRESHOLD_MS = 3000L
        const val REQUEST_ID_HEADER = "X-Request-Id"
    }

    // Google Places API
    object GooglePlaces {
        const val DEFAULT_TIMEOUT_SECONDS = 10L
        const val MAX_RETRIES = 3L
        const val RETRY_DELAY_SECONDS = 1L
        const val COLLECTION_DELAY_MS = 2000L
        const val MAX_RESULTS = 20

        val FIELD_MASK = listOf(
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.nationalPhoneNumber",
            "places.regularOpeningHours",
            "places.rating"
        ).joinToString(",")
    }

    object SeoulLocations {
        val GRID_POINTS = listOf(
            Location("강남역", 37.4979, 127.0276),
            Location("선릉역", 37.5048, 127.0493),
            Location("삼성역", 37.5087, 127.0633),
            Location("역삼역", 37.5007, 127.0361),
            Location("교대역", 37.4933, 127.0143),
            Location("잠실역", 37.5133, 127.1000),
            Location("홍대입구역", 37.5571, 126.9245),
            Location("이태원역", 37.5345, 126.9945),
            Location("광화문", 37.5720, 126.9769),
            Location("명동역", 37.5635, 126.9825),
            Location("왕십리역", 37.5610, 127.0374),
            Location("성수역", 37.5444, 127.0557),
            Location("건대입구역", 37.5405, 127.0701),
            Location("신촌역", 37.5553, 126.9369),
            Location("여의도역", 37.5219, 126.9245),
            Location("사당역", 37.4765, 126.9816)
        )

        data class Location(
            val name: String,
            val latitude: Double,
            val longitude: Double
        )
    }
}
