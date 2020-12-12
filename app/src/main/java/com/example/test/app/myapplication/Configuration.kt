package com.example.test.app.myapplication

import com.bhargavms.mappergen.annotations.Mapper

interface Configuration {
    @Mapper
    fun map(value: Network.HotelDetailsPOI): Domain.HotelDetailPOI
}
