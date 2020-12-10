package com.bhargavms.test

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers

infix fun Any.shouldBeEqualTo(other: Any) {
    assertThat(
        "$this should be equal to $other",
        this,
        Matchers.equalTo(other)
    )
}