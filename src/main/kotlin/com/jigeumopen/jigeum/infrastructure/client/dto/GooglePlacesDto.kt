package com.jigeumopen.jigeum.infrastructure.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class SearchNearbyRequest(
    val includedTypes: List<String> = listOf("cafe"),
    val maxResultCount: Int = 20,
    val locationRestriction: LocationRestriction,
    val languageCode: String = "ko"
)

data class LocationRestriction(
    val circle: Circle
)

data class Circle(
    val center: LatLng,
    val radius: Double
)

data class LatLng(
    @JsonProperty("latitude")
    val latitude: Double,
    @JsonProperty("longitude")
    val longitude: Double
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchNearbyResponse(
    @JsonProperty("places")
    val places: List<Place>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Place(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("displayName")
    val displayName: LocalizedText? = null,

    @JsonProperty("formattedAddress")
    val formattedAddress: String? = null,

    @JsonProperty("location")
    val location: LatLng? = null,

    @JsonProperty("regularOpeningHours")
    val regularOpeningHours: RegularOpeningHours? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalizedText(
    @JsonProperty("text")
    val text: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegularOpeningHours(
    @JsonProperty("periods")
    val periods: List<Period>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Period(
    @JsonProperty("open")
    val open: DayTime,

    @JsonProperty("close")
    val close: DayTime
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DayTime(
    @JsonProperty("day")
    val day: Int,

    @JsonProperty("hour")
    val hour: Int,

    @JsonProperty("minute")
    val minute: Int
)
