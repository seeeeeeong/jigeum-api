package com.jigeumopen.jigeum.external.dto

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
    val latitude: Double,
    val longitude: Double
)

// 응답
data class SearchNearbyResponse(
    val places: List<Place>?
)

data class Place(
    val id: String,
    val displayName: LocalizedText?,
    val formattedAddress: String?,
    val location: LatLng?,
    val nationalPhoneNumber: String?,
    val regularOpeningHours: RegularOpeningHours?,
    val rating: Double?
)

data class LocalizedText(
    val text: String?,
    val languageCode: String?
)

data class RegularOpeningHours(
    val periods: List<Period>?
)

data class Period(
    val open: DayTime?,
    val close: DayTime?
)

data class DayTime(
    val day: Int?,
    val hour: Int?,
    val minute: Int?
)
