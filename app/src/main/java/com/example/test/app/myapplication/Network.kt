package com.example.test.app.myapplication

class Network {
    data class AttractionEntity(
        val name: String?,
        val distance: Double?,
        val type: HotelAttractionType?,
        val latitude: Double?,
        val longitude: Double?
    )

    enum class HotelAttractionType {
        LANDMARK,
        AIRPORT,
        TRANSPORT,
        BASECAMP
    }

    data class BasecampAttractionEntity(
        val name: String?,
        val distance: Double?,
        val attractionType: HotelAttractionType?,
        val latitude: Double?,
        val longitude: Double?,
        val basecampDetails: BasecampDetailsEntity?
    )

    data class BasecampDetailsEntity(
        val basecampType: Int?,
        val reviewScore: Double?,
        val reviewCount: Int?
    )

    data class HotelDetailsPOI(
        val attractions: List<AttractionEntity>,
        val baseCamps: List<BasecampAttractionEntity>
    )
}