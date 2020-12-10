package com.example.test.app.myapplication

class Domain {
    data class AttractionEntity(
        val name: String,
        val distance: Double,
        val type: HotelAttractionType,
        val latitude: Double,
        val longitude: Double
    )

    enum class HotelAttractionType {
        LANDMARK,
        AIRPORT,
        TRANSPORT,
        BASECAMP
    }

    data class BasecampAttraction(
        val name: String,
        var distance: Double = 0.0,
        var attractionType: HotelAttractionType? = null,
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var basecampDetails: BasecampDetails? = null
    )

    data class BasecampDetails(
        var basecampType: Int = 0,
        var reviewCount: Int = 0,
        var reviewScore: Double = 0.0
    )

    data class HotelDetailPOI(
        var hotelAttractionList: List<AttractionEntity>,
        var basecampAttractionList: List<BasecampAttraction>
    )
}