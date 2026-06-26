package com.example

public fun mapPrimitivesDtoToPrimitives(input: Network.PrimitivesDto?): Domain.Primitives? =
    input?.let {
        Domain.Primitives(
            byteValue = it.byteValue ?: 0,
            shortValue = it.shortValue ?: 0,
            intValue = it.intValue ?: 0,
            longValue = it.longValue ?: 0L,
            floatValue = it.floatValue ?: 0.0f,
            doubleValue = it.doubleValue ?: 0.0,
            booleanValue = it.booleanValue ?: false,
            charValue = it.charValue ?: '\u0000',
            stringValue = it.stringValue ?: "",
        )
    }
