package com.bhargavms.mappergen.testsupport

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.lang.reflect.Proxy

object MinimalKspStubs {
    val unusedClassDeclaration: KSClassDeclaration by lazy {
        @Suppress("UNCHECKED_CAST")
        Proxy.newProxyInstance(
            KSClassDeclaration::class.java.classLoader,
            arrayOf(KSClassDeclaration::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getAllProperties" -> emptySequence<KSPropertyDeclaration>()
                "toString" -> "StubClassDeclaration"
                "hashCode" -> 0
                else -> defaultValue(method.returnType)
            }
        } as KSClassDeclaration
    }

    val unusedPropertyDeclaration: KSPropertyDeclaration by lazy {
        @Suppress("UNCHECKED_CAST")
        Proxy.newProxyInstance(
            KSPropertyDeclaration::class.java.classLoader,
            arrayOf(KSPropertyDeclaration::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "toString" -> "StubPropertyDeclaration"
                "hashCode" -> 0
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
