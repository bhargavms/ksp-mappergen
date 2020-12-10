package com.example.test.app.myapplication

import com.bhargavms.mappergen.annotations.Mapper

@Mapper
interface Configuration {
    fun map(value: Network.HotelDetailsPOI): Domain.HotelDetailPOI
}
