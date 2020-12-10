package com.bhargavms.mappergen.code.generator

import com.bhargavms.test.shouldBeEqualTo
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class AssignmentTest {
    private lateinit var directAssignment: Assignment
    private lateinit var mappedAssignment: Assignment

    @Mock
    lateinit var inputField: KSPropertyDeclaration

    @Mock
    lateinit var outputField: KSPropertyDeclaration

    @Mock
    lateinit var inputType: KSType

    @Mock
    lateinit var outputType: KSType

    @Mock
    internal lateinit var mapFunctionResolver: MapFunctionResolver

    @Before
    fun setUp() {
        directAssignment =
            Assignment.DirectAssignment(AssignmentDeclaration(inputField, outputField))
//        mappedAssignment = Assignment.MappedAssignment(
//            AssignmentDeclaration(inputField, outputField),
//            map
//        )
    }

    @Test
    fun `direct assignment generates direct assignment from input to output`() {
        val inputTypeRef = mock<KSTypeReference> {
            on { resolve() } doReturn inputType
        }
        doReturn(inputTypeRef).whenever(inputField).type
        val outputTypeRef = mock<KSTypeReference> {
            on { resolve() } doReturn outputType
        }
        doReturn(outputTypeRef).whenever(outputField).type
        doReturn(mock<KSName> {
            on { asString() } doReturn "input"
        }).whenever(inputField).simpleName
        doReturn(mock<KSName> {
            on { asString() } doReturn "output"
        }).whenever(outputField).simpleName
        doReturn(true).whenever(outputType).isAssignableFrom(inputType)

        val generatedString = directAssignment.generateStatement()

        generatedString.toString() shouldBeEqualTo "output = input"
    }
}