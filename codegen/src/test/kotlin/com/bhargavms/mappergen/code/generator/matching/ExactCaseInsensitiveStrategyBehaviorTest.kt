package com.bhargavms.mappergen.code.generator.matching

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class ExactCaseInsensitiveStrategyBehaviorTest {
    @Test
    fun `findMatchingProperty matches case insensitive names`() {
        val sourceClass = classWithProperties("UserName" to "UserName")
        val match = ExactCaseInsensitiveStrategy.findMatchingProperty(sourceClass, "username")
        assertNotNull(match)
        assertEquals("UserName", match?.simpleName?.asString())
    }

    @Test
    fun `findMatchingProperty returns null when no match exists`() {
        val sourceClass = classWithProperties("id" to "id")
        val match = ExactCaseInsensitiveStrategy.findMatchingProperty(sourceClass, "name")
        assertEquals(null, match)
    }

    @Test
    fun `findMatchingProperty returns first case insensitive match`() {
        val sourceClass = classWithProperties("NAME" to "NAME", "name" to "name")
        val match = ExactCaseInsensitiveStrategy.findMatchingProperty(sourceClass, "name")
        assertNotNull(match)
        assertEquals("NAME", match?.simpleName?.asString())
    }

    private fun classWithProperties(vararg properties: Pair<String, String>): KSClassDeclaration {
        val propertyDeclarations = properties.map { (name, _) -> propertyNamed(name) }
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            KSClassDeclaration::class.java.classLoader,
            arrayOf(KSClassDeclaration::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getAllProperties" -> propertyDeclarations.asSequence()
                "toString" -> "PropertyContainer"
                "hashCode" -> propertyDeclarations.hashCode()
                else -> defaultValue(method.returnType)
            }
        } as KSClassDeclaration
    }

    private fun propertyNamed(name: String): KSPropertyDeclaration {
        val ksName =
            Proxy.newProxyInstance(
                KSName::class.java.classLoader,
                arrayOf(KSName::class.java),
            ) { _, method, _ ->
                when (method.name) {
                    "asString" -> name
                    "getShortName" -> name
                    "getQualifier" -> ""
                    "toString" -> name
                    "hashCode" -> name.hashCode()
                    else -> defaultValue(method.returnType)
                }
            } as KSName

        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            KSPropertyDeclaration::class.java.classLoader,
            arrayOf(KSPropertyDeclaration::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getSimpleName" -> ksName
                "toString" -> name
                "hashCode" -> name.hashCode()
                else -> defaultValue(method.returnType)
            }
        } as KSPropertyDeclaration
    }

    private fun defaultValue(returnType: Class<*>): Any? =
        when (returnType.name) {
            "boolean" -> false
            "int" -> 0
            "long" -> 0L
            "short" -> 0.toShort()
            "byte" -> 0.toByte()
            "char" -> 0.toChar()
            "float" -> 0f
            "double" -> 0.0
            else -> null
        }
}
