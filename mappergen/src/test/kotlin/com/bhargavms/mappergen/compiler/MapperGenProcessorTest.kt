package com.bhargavms.mappergen.compiler

import com.bhargavms.mappergen.annotations.Mapper
import com.bhargavms.mappergen.compiler.errors.BadAnnotationTargetException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MapperGenProcessorTest {
    lateinit var mapperGenProcessor: MapperGenProcessor

    @Mock
    lateinit var resolver: Resolver

    @Mock
    lateinit var codeGenerator: CodeGenerator
    @Mock
    lateinit var logger: KSPLogger
    @Mock
    lateinit var propertyAccessor: KSPropertyAccessor
    @Mock
    lateinit var classDec: KSClassDeclaration

    @Before
    fun setUp() {
        mapperGenProcessor = MapperGenProcessor()
        mapperGenProcessor.init(mapOf(), KotlinVersion(1, 4), codeGenerator, logger)
    }

    @Test
    fun `ensure that process does not proceed when lateinit vars are not initialized`() {
        mapperGenProcessor = MapperGenProcessor()
        mapperGenProcessor.process(resolver)

        verifyZeroInteractions(resolver)
    }

    @Test(expected = BadAnnotationTargetException::class)
    fun `onProcess throws when annotation mapper is not on interface or class declaration`() {
        whenever(resolver.getSymbolsWithAnnotation(Mapper::class.qualifiedName!!))
            .thenReturn(listOf<KSAnnotated>(propertyAccessor, classDec))

        mapperGenProcessor.process(resolver)
    }
}
