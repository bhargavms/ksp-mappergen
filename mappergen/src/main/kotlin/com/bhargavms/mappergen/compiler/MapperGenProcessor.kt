package com.bhargavms.mappergen.compiler

import com.bhargavms.mappergen.annotations.Mapper
import com.bhargavms.mappergen.code.generator.MapFunctionDeclaration
import com.bhargavms.mappergen.code.generator.generate
import com.bhargavms.mappergen.compiler.errors.BadAnnotationTargetException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class MapperGenProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        MapperGenProcessor(environment.codeGenerator, environment.logger)
}

class MapperGenProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver
            .getSymbolsWithAnnotation(Mapper::class.qualifiedName!!)
            .map {
                (it as? KSFunctionDeclaration)
                    ?: throw BadAnnotationTargetException(Mapper::class, "function")
            }.forEach { declaration ->
                generateMappersForInterface(declaration) {
                    this?.generate(
                        resolver,
                        codeGenerator,
                        declaration.packageName.asString(),
                    )
                }
            }
        return emptyList()
    }

    override fun finish() {}

    companion object {
        private inline fun generateMappersForInterface(
            declaration: KSFunctionDeclaration,
            generate: MapFunctionDeclaration?.() -> Unit,
        ) {
            declaration.extract().generate()
        }
    }
}

private fun KSFunctionDeclaration.extract(): MapFunctionDeclaration? {
    val params = parameters
    val returnType = returnType

    if (params.size == 1 && returnType != null) {
        return MapFunctionDeclaration(params[0].type.resolve(), returnType.resolve())
    } else {
        return null
    }
}
