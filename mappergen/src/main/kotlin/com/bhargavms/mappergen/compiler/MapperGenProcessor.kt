package com.bhargavms.mappergen.compiler

import com.bhargavms.mappergen.annotations.Mapper
import com.bhargavms.mappergen.code.generator.MapFunctionDeclaration
import com.bhargavms.mappergen.code.generator.generate
import com.bhargavms.mappergen.compiler.errors.BadAnnotationTargetException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class MapperGenProcessor : SymbolProcessor {
    private lateinit var realProcessor: RealProcessor
    override fun finish() {
    }

    override fun init(
        options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        realProcessor = RealProcessor(codeGenerator)
    }

    override fun process(resolver: Resolver) {
        if (this::realProcessor.isInitialized.not()) return
        realProcessor.process(resolver)
    }

    private class RealProcessor(
        val codeGenerator: CodeGenerator
    ) {
        fun process(resolver: Resolver) {
            resolver.getSymbolsWithAnnotation(Mapper::class.qualifiedName!!)
                    .map {
                        (it as? KSFunctionDeclaration)
                            ?: throw BadAnnotationTargetException(Mapper::class, "function")
                    }
                    .forEach { declaration ->
                        generateMappersForInterface(declaration) {
                            this?.generate(
                                resolver, codeGenerator,
                                declaration.packageName.asString()
                            )
                        }
                    }
        }
    }

    companion object {
        private inline fun generateMappersForInterface(
            declaration: KSFunctionDeclaration,
            generate: MapFunctionDeclaration?.() -> Unit
        ) {
            declaration.extract().generate()
        }
    }
}

private fun KSFunctionDeclaration.extract(): MapFunctionDeclaration? {
    val params = parameters
    val returnType = returnType

    if (params.size == 1 && returnType != null) {
        return MapFunctionDeclaration(params[0].type!!.resolve(), returnType.resolve())
    } else {
        return null
    }
}