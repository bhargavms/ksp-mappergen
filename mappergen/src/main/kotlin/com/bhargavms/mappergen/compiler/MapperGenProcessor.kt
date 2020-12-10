package com.bhargavms.mappergen.compiler

import com.bhargavms.mappergen.annotations.Mapper
import com.bhargavms.mappergen.compiler.errors.BadAnnotationTargetException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSClassDeclaration

class MapperGenProcessor : SymbolProcessor {
    private lateinit var realProcessor: RealProcessor
    override fun finish() {
    }

    override fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger) {
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
                        (it as? KSClassDeclaration)
                                ?: throw BadAnnotationTargetException(Mapper::class, "interface/class")
                    }
                    .map {
//                        generate()
                    }
        }
    }
}
