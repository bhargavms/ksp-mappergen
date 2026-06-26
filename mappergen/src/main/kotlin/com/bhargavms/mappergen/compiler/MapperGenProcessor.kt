package com.bhargavms.mappergen.compiler

import com.bhargavms.mappergen.annotations.Mapper
import com.bhargavms.mappergen.code.generator.MapFunctionDeclaration
import com.bhargavms.mappergen.code.generator.PropertyTransformConfig
import com.bhargavms.mappergen.code.generator.generate
import com.bhargavms.mappergen.code.generator.matching.MatchingStrategyType
import com.bhargavms.mappergen.compiler.errors.BadAnnotationTargetException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType

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
        val mapperAnnotation =
            annotations.firstOrNull {
                it.shortName.asString() == "Mapper"
            }

        val matchingStrategy = mapperAnnotation?.extractMatchingStrategy() ?: MatchingStrategyType.EXACT
        val transforms = mapperAnnotation?.extractTransforms() ?: emptyMap()

        return MapFunctionDeclaration(
            input = params[0].type.resolve(),
            output = returnType.resolve(),
            matchingStrategy = matchingStrategy,
            propertyTransforms = transforms,
        )
    } else {
        return null
    }
}

/**
 * Extract the matching strategy from the @Mapper annotation.
 */
private fun KSAnnotation.extractMatchingStrategy(): MatchingStrategyType {
    val strategyArg = arguments.firstOrNull { it.name?.asString() == "matchingStrategy" }
    val strategyValue = strategyArg?.value

    return when (strategyValue) {
        is KSType -> {
            // KSP1: enum entries are represented as KSType
            val enumName = strategyValue.declaration.simpleName.asString()
            MatchingStrategyType.entries.firstOrNull { it.name == enumName } ?: MatchingStrategyType.EXACT
        }
        is KSClassDeclaration -> {
            // KSP2: enum entries are represented as KSClassDeclaration
            val enumName = strategyValue.simpleName.asString()
            MatchingStrategyType.entries.firstOrNull { it.name == enumName } ?: MatchingStrategyType.EXACT
        }
        null -> MatchingStrategyType.EXACT
        else -> {
            val enumName = strategyValue.toString()
            MatchingStrategyType.entries.firstOrNull { it.name == enumName || enumName.endsWith(".${it.name}") }
                ?: MatchingStrategyType.EXACT
        }
    }
}

/**
 * Extract property transforms from the @Mapper annotation.
 */
@Suppress("UNCHECKED_CAST")
private fun KSAnnotation.extractTransforms(): Map<String, PropertyTransformConfig> {
    val transformsArg = arguments.firstOrNull { it.name?.asString() == "transforms" }
    val transformsValue = transformsArg?.value as? List<KSAnnotation> ?: return emptyMap()

    return transformsValue
        .mapNotNull { transformAnnotation ->
            val target =
                transformAnnotation.arguments
                    .firstOrNull { it.name?.asString() == "target" }
                    ?.value as? String
                    ?: return@mapNotNull null

            val source =
                transformAnnotation.arguments
                    .firstOrNull { it.name?.asString() == "source" }
                    ?.value as? String

            val expression =
                transformAnnotation.arguments
                    .firstOrNull { it.name?.asString() == "expression" }
                    ?.value as? String

            target to
                PropertyTransformConfig(
                    sourceProperty = source?.takeIf { it.isNotEmpty() },
                    expression = expression?.takeIf { it.isNotEmpty() },
                )
        }.toMap()
}
