package com.bhargavms.mappergen.code.generator

import org.junit.Test
import java.lang.IllegalArgumentException

class ContractKtTest {

    @Test(expected = IllegalArgumentException::class)
    fun `generate should fail when inputs are not data classes`() {
//        generate(A::class, B::class)
    }
}

private class A
private class B
