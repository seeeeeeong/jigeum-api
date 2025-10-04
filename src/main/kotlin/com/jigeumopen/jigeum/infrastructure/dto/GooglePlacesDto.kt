package com.jigeumopen.jigeum.infrastructure.dto

// ===== Request DTOs =====

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

// ===== Response DTOs =====

data class SearchNearbyResponse(
    val places: List<Place>? = null
)

data class Place(
    val id: String,
    val displayName: LocalizedText? = null,
    val formattedAddress: String? = null,
    val location: LatLng? = null,
    val nationalPhoneNumber: String? = null,
    val regularOpeningHours: RegularOpeningHours? = null,
    val rating: Double? = null
)

data class LocalizedText(
    val text: String? = null,
    val languageCode: String? = null
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

data class RegularOpeningHours(
    val periods: List<Period>? = null
)

data class Period(
    val open: DayTime? = null,
    val close: DayTime? = null
)

data class DayTime(
    val day: Int? = null,
    val hour: Int? = null,
    val minute: Int? = null
)
