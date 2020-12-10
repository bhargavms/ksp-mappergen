package com.bhargavms.mappergen.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class KSInterfaceProcessorTest {

    private lateinit var interfaceProcessor: InterfaceProcessor

    @Mock
    lateinit var methodProcessor: MethodProcessor

    @Mock
    lateinit var ksInterface: KSClassDeclaration

    @Mock
    lateinit var function1: KSFunctionDeclaration

    @Mock
    lateinit var function2: KSFunctionDeclaration

    @Before
    fun setUp() {
        interfaceProcessor = InterfaceProcessor(methodProcessor)
    }

    @Test
    fun `given an KSInterface extract all functions and pass them down for further processing`() {
        val functions = listOf(function1, function2)
        whenever(ksInterface.getAllFunctions()).thenReturn(functions)

        interfaceProcessor.process(ksInterface)

        functions.forEach {
            verify(methodProcessor).process(it)
        }
    }
}