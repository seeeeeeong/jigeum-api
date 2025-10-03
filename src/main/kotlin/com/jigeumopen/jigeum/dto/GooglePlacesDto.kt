package com.jigeumopen.jigeum.external.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GooglePlacesResponse(
    val results: List<GooglePlace>,
    @JsonProperty("next_page_token")
    val nextPageToken: String?
)

data class GooglePlace(
    @JsonProperty("place_id")
    val placeId: String,
    val name: String,
    val vicinity: String,
    val geometry: GoogleGeometry
)

data class GooglePlaceDetailsResponse(
    val result: GooglePlaceDetail
)

data class GooglePlaceDetail(
    val name: String,
    @JsonProperty("formatted_address")
    val formattedAddress: String?,
    @JsonProperty("formatted_phone_number")
    val formattedPhoneNumber: String?,
    val geometry: GoogleGeometry,
    @JsonProperty("opening_hours")
    val openingHours: GoogleOpeningHours?,
    val rating: Double?
)

data class GoogleGeometry(
    val location: GoogleLocation
)

data class GoogleLocation(
    val lat: Double,
    val lng: Double
)

data class GoogleOpeningHours(
    val periods: List<GooglePeriod>?,
    @JsonProperty("weekday_text")
    val weekdayText: List<String>?
)

data class GooglePeriod(
    val close: GoogleTime?,
    val open: GoogleTime?
)

data class GoogleTime(
    val day: Int,
    val time: String
)
