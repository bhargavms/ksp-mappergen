package com.bhargavms.mappergen.code.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FileSpec
import java.io.OutputStreamWriter
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets.UTF_8

interface Generate {
    operator fun invoke()
}

interface GenerateMapperFile : Generate

fun MapFunctionDeclaration.generate(
    resolver: Resolver,
    codeGenerator: CodeGenerator,
    packageName: String,
) {
    if (input.declaration is KSClassDeclaration && output.declaration is KSClassDeclaration) {
        MapperFile(
            packageName,
            this,
            resolver,
            codeGenerator,
        ).invoke()
    } else {
        throw RuntimeException()
    }
}

internal interface MapFunctionResolver {
    fun resolveRequiredMapFunction(mapFunctionDeclaration: MapFunctionDeclaration)
}

internal interface CollectionTypeCheckHelper {
    fun isIterable(type: KSType): Boolean

    fun isArray(type: KSType): Boolean
}

internal class MapperFile(
    private val packageName: String,
    private val mapFunctionDeclaration: MapFunctionDeclaration,
    private val resolver: Resolver,
    private val codeGenerator: CodeGenerator,
) : GenerateMapperFile,
    MapFunctionResolver,
    CollectionTypeCheckHelper {
    private val mapFunctions: MutableMap<MapFunctionDeclaration, MapFunction> =
        mutableMapOf(
            mapFunctionDeclaration to
                MapFunction.create(
                    mapFunctionDeclaration,
                    this,
                    this,
                ),
        )

    private fun FileSpec.writeTo(codeGenerator: CodeGenerator) {
        OutputStreamWriter(
            codeGenerator.createNewFile(Dependencies(false), packageName, name),
            UTF_8,
        ).use(::writeTo)
    }

    override operator fun invoke() {
        FileSpec
            .builder(
                packageName,
                getMapFunctionName(mapFunctionDeclaration.input, mapFunctionDeclaration.output),
            ).apply {
                mapFunctions
                    .map {
                        it.value.generateFunction()
                    }.forEach {
                        this.addFunction(it)
                    }
            }.build()
            .writeTo(codeGenerator)
    }

    override fun resolveRequiredMapFunction(mapFunctionDeclaration: MapFunctionDeclaration) {
        if (!mapFunctions.contains(mapFunctionDeclaration)) {
            mapFunctions[mapFunctionDeclaration] =
                MapFunction.create(mapFunctionDeclaration, this, this)
        }
    }

    override fun isIterable(type: KSType): Boolean = resolver.builtIns.iterableType.isAssignableFrom(type)

    override fun isArray(type: KSType) = resolver.builtIns.arrayType.isAssignableFrom(type)
}
