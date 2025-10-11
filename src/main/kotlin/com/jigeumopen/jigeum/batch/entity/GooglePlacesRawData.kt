package com.jigeumopen.jigeum.batch.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.jigeumopen.jigeum.cafe.dto.Place
import com.jigeumopen.jigeum.common.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "google_places_raw_data")
class GooglePlacesRawData(
    @Column(name = "place_id", nullable = false, length = 100)
    val placeId: String,

    @Column(name = "batch_id", nullable = false, length = 50)
    val batchId: String,

    @Column(name = "display_name", length = 200)
    val displayName: String?,

    @Column(name = "formatted_address", length = 500)
    val formattedAddress: String?,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours", columnDefinition = "jsonb")
    val openingHours: String?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    val rawData: String,

    @Column(nullable = false)
    var processed: Boolean = false,

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null
) : BaseEntity() {

    fun markAsProcessed() {
        this.processed = true
    }

    fun markAsError(message: String) {
        this.errorMessage = message
    }

    companion object {
        fun fromPlace(place: Place, batchId: String, objectMapper: ObjectMapper): GooglePlacesRawData {
            return GooglePlacesRawData(
                placeId = place.id,
                batchId = batchId,
                displayName = place.displayName?.text,
                formattedAddress = place.formattedAddress,
                latitude = place.location?.latitude ?: 0.0,
                longitude = place.location?.longitude ?: 0.0,
                openingHours = place.regularOpeningHours?.let { objectMapper.writeValueAsString(it) },
                rawData = objectMapper.writeValueAsString(place),
                processed = false
            )
        }
    }
}
