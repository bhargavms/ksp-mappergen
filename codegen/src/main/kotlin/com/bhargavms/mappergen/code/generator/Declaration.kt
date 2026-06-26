package com.bhargavms.mappergen.code.generator

import com.bhargavms.mappergen.code.generator.matching.MatchingStrategyType
import com.bhargavms.mappergen.code.generator.matching.PropertyMatchingStrategy
import com.bhargavms.mappergen.code.generator.utils.typeName
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.NonExistLocation
import com.squareup.kotlinpoet.FunSpec

sealed class Declaration

data class MapFunctionDeclaration(
    val input: KSType,
    val output: KSType,
    val matchingStrategy: MatchingStrategyType = MatchingStrategyType.EXACT,
    val propertyTransforms: Map<String, PropertyTransformConfig> = emptyMap(),
) : Declaration()

/**
 * Configuration for a custom property transformation.
 *
 * @param sourceProperty The name of the source property (or null to use the target property name)
 * @param transformerClass The fully qualified class name of the transformer to use
 * @param expression An optional inline expression (e.g., "it.firstName + \" \" + it.lastName")
 */
data class PropertyTransformConfig(
    val sourceProperty: String? = null,
    val transformerClass: String? = null,
    val expression: String? = null,
)

internal data class AssignmentDeclaration(
    val from: KSPropertyDeclaration,
    val to: KSPropertyDeclaration,
) : Declaration()

/**
 * Declaration for a custom transformed assignment using an inline expression.
 */
internal data class TransformedAssignmentDeclaration(
    val to: KSPropertyDeclaration,
    val expression: String,
    val sourceProperty: String? = null,
) : Declaration()

internal abstract class MapFunction(
    protected val mapFunctionDeclaration: MapFunctionDeclaration,
    private val mapFunctionResolver: MapFunctionResolver,
    private val typeCheckHelper: CollectionTypeCheckHelper,
) {
    abstract fun generateFunction(): FunSpec

    protected val mapFunctionName: String by lazy {
        getMapFunctionName(
            mapFunctionDeclaration.input,
            mapFunctionDeclaration.output,
        )
    }

    private val matchingStrategy: PropertyMatchingStrategy by lazy {
        mapFunctionDeclaration.matchingStrategy.toStrategy()
    }

    protected val assignments: List<AssignmentGenerator> by lazy {
        mapFunctionResolver.beginAssignmentGeneration(mapFunctionDeclaration)
        try {
            val inputClass = mapFunctionDeclaration.input.declaration as KSClassDeclaration
            val outputClass = mapFunctionDeclaration.output.declaration as KSClassDeclaration
            val transforms = mapFunctionDeclaration.propertyTransforms

            outputClass
                .getAllProperties()
                .map { outputProperty ->
                    val outputName = outputProperty.simpleName.asString()
                    val transformConfig = transforms[outputName]

                    if (transformConfig?.expression != null) {
                        TransformedAssignmentDeclaration(
                            to = outputProperty,
                            expression = transformConfig.expression,
                            sourceProperty = transformConfig.sourceProperty,
                        )
                    } else {
                        val sourcePropertyName = transformConfig?.sourceProperty ?: outputName
                        val matchingInputProperty =
                            matchingStrategy.findMatchingProperty(inputClass, sourcePropertyName)
                                ?: throw IllegalStateException(
                                    "Cannot map property '$outputName': no matching source property found " +
                                        "in '${inputClass.simpleName.asString()}'.",
                                )

                        AssignmentDeclaration(from = matchingInputProperty, to = outputProperty)
                    }
                }.map { declaration ->
                    when (declaration) {
                        is AssignmentDeclaration ->
                            Assignment.create(
                                declaration,
                                mapFunctionResolver,
                                typeCheckHelper,
                                mapFunctionDeclaration.matchingStrategy,
                            )
                        is TransformedAssignmentDeclaration -> TransformedAssignment(declaration, typeCheckHelper)
                        is MapFunctionDeclaration -> error("Unexpected MapFunctionDeclaration in assignment list")
                    }
                }.toList()
        } finally {
            mapFunctionResolver.endAssignmentGeneration(mapFunctionDeclaration)
        }
    }

    companion object {
        fun create(
            mapFunctionDeclaration: MapFunctionDeclaration,
            mapFunctionResolver: MapFunctionResolver,
            typeCheckHelper: CollectionTypeCheckHelper,
        ): MapFunction =
            if (typeCheckHelper.isArray(mapFunctionDeclaration.output) ||
                typeCheckHelper.isIterable(mapFunctionDeclaration.output)
            ) {
                IterableObjectMapFunction(mapFunctionDeclaration, mapFunctionResolver, typeCheckHelper)
            } else {
                PlainObjectMapFunction(mapFunctionDeclaration, mapFunctionResolver, typeCheckHelper)
            }
    }
}

internal class IterableObjectMapFunction(
    mapFunctionDeclaration: MapFunctionDeclaration,
    mapFunctionResolver: MapFunctionResolver,
    typeCheckHelper: CollectionTypeCheckHelper,
) : MapFunction(mapFunctionDeclaration, mapFunctionResolver, typeCheckHelper) {
    override fun generateFunction(): FunSpec {
        val itemMapFunctionName =
            getMapFunctionName(
                mapFunctionDeclaration.input.arguments
                    .first()
                    .type!!
                    .resolve(),
                mapFunctionDeclaration.output.arguments
                    .first()
                    .type!!
                    .resolve(),
            )
        return FunSpec
            .builder(mapFunctionName)
            .addParameter("input", mapFunctionDeclaration.input.typeName().copy(true))
            .returns(mapFunctionDeclaration.output.typeName().copy(true))
            .addCode("return input?.mapNotNull { $itemMapFunctionName(it) }")
            .build()
    }
}

internal class PlainObjectMapFunction(
    mapFunctionDeclaration: MapFunctionDeclaration,
    mapFunctionResolver: MapFunctionResolver,
    typeCheckHelper: CollectionTypeCheckHelper,
) : MapFunction(mapFunctionDeclaration, mapFunctionResolver, typeCheckHelper) {
    override fun generateFunction(): FunSpec {
        val outputTypeName = mapFunctionDeclaration.output.typeName()

        val assignmentStatements =
            assignments.mapIndexed { index, assignment ->
                val separator = if (index < assignments.size - 1) "," else ""
                assignment() + separator
            }

        return FunSpec
            .builder(mapFunctionName)
            .addParameter("input", mapFunctionDeclaration.input.typeName().copy(true))
            .returns(outputTypeName.copy(nullable = true))
            .apply {
                addCode("return input?.let {\n")
                addCode("    %T(\n", outputTypeName)
                assignmentStatements.forEach { arg ->
                    addCode("        $arg\n")
                }
                addCode("    )\n")
                addCode("}")
            }.build()
    }
}

fun getMapFunctionName(
    fromType: KSType,
    toType: KSType,
): String = "map${getName(fromType)}To${getName(toType)}"

fun getName(type: KSType): String {
    val builder = StringBuilder()
    type.arguments.forEach {
        it.type?.let { builder.append(getName(it.resolve())) }
    }
    builder.append(type.declaration.simpleName.asString())
    return builder.toString()
}

private fun KSPropertyDeclaration.locationString(): String =
    when (val loc = location) {
        is FileLocation -> "${loc.filePath}:${loc.lineNumber}"
        is NonExistLocation -> "<unknown location>"
    }

private fun isCollectionLike(
    type: KSType,
    typeCheckHelper: CollectionTypeCheckHelper,
): Boolean {
    if (typeCheckHelper.isMap(type)) {
        return false
    }
    val qualifiedName = type.declaration.qualifiedName?.asString() ?: ""
    return typeCheckHelper.isIterable(type) ||
        typeCheckHelper.isArray(type) ||
        qualifiedName.startsWith("kotlin.collections.")
}

internal fun needsTypeMapping(
    fromType: KSType,
    toType: KSType,
    typeCheckHelper: CollectionTypeCheckHelper,
): Boolean {
    if (fromType.declaration.qualifiedName?.asString() != toType.declaration.qualifiedName?.asString()) {
        return true
    }
    if (fromType.arguments.size != toType.arguments.size) {
        return true
    }
    for (index in fromType.arguments.indices) {
        val fromArgument = fromType.arguments[index].type?.resolve() ?: return true
        val toArgument = toType.arguments[index].type?.resolve() ?: return true
        if (needsTypeMapping(fromArgument, toArgument, typeCheckHelper)) {
            return true
        }
    }
    return false
}

private fun buildCollectionMappingExpression(
    fromType: KSType,
    toType: KSType,
    accessExpr: String,
    matchingStrategy: MatchingStrategyType,
    mapFunctionResolver: MapFunctionResolver,
    typeCheckHelper: CollectionTypeCheckHelper,
): String {
    val fromElementType =
        fromType.arguments
            .firstOrNull()
            ?.type
            ?.resolve()
            ?: error("Collection type is missing an element type")
    val toElementType =
        toType.arguments
            .firstOrNull()
            ?.type
            ?.resolve()
            ?: error("Collection type is missing an element type")

    if (isCollectionLike(fromElementType, typeCheckHelper) &&
        isCollectionLike(toElementType, typeCheckHelper) &&
        needsTypeMapping(fromElementType, toElementType, typeCheckHelper)
    ) {
        mapFunctionResolver.resolveRequiredMapFunction(
            MapFunctionDeclaration(fromElementType, toElementType, matchingStrategy),
        )
        val innerMapping =
            buildCollectionMappingExpression(
                fromElementType,
                toElementType,
                "inner",
                matchingStrategy,
                mapFunctionResolver,
                typeCheckHelper,
            )
        return "$accessExpr.map { inner -> $innerMapping }"
    }

    mapFunctionResolver.resolveRequiredMapFunction(
        MapFunctionDeclaration(fromElementType, toElementType, matchingStrategy),
    )
    val elementMapFunctionName = getMapFunctionName(fromElementType, toElementType)
    return "$accessExpr.mapNotNull { $elementMapFunctionName(it) }"
}

/**
 * Common interface for all assignment generators (regular and transformed).
 */
internal interface AssignmentGenerator {
    operator fun invoke(): String
}

internal sealed class Assignment(
    protected val assignmentDeclaration: AssignmentDeclaration,
    protected val typeCheckHelper: CollectionTypeCheckHelper,
) : AssignmentGenerator {
    abstract override operator fun invoke(): String

    internal class DirectAssignment(
        assignmentDeclaration: AssignmentDeclaration,
        typeCheckHelper: CollectionTypeCheckHelper,
    ) : Assignment(assignmentDeclaration, typeCheckHelper) {
        override fun invoke(): String {
            val fromName = assignmentDeclaration.from.simpleName.asString()
            val toName = assignmentDeclaration.to.simpleName.asString()
            val fromType = assignmentDeclaration.from.type.resolve()
            val toType = assignmentDeclaration.to.type.resolve()

            if (typeCheckHelper.isMap(fromType) && typeCheckHelper.isMap(toType)) {
                val mapExpression = "it.$fromName?.entries?.associate { it.key to it.value }"
                return if (fromType.isMarkedNullable && !toType.isMarkedNullable) {
                    "$toName = $mapExpression ?: emptyMap()"
                } else {
                    "$toName = $mapExpression"
                }
            }

            // If source is nullable but target is not, add default value
            if (fromType.isMarkedNullable && !toType.isMarkedNullable) {
                val defaultValue = getDefaultValue(toType)
                return "$toName = it.$fromName ?: $defaultValue"
            } else {
                // Direct assignment
                return "$toName = it.$fromName"
            }
        }

        private fun getDefaultValue(type: KSType): String {
            // Check if it's a collection type
            if (typeCheckHelper.isIterable(type)) {
                return "emptyList()"
            }
            if (typeCheckHelper.isMap(type)) {
                return "emptyMap()"
            }
            if (typeCheckHelper.isArray(type)) {
                val elementType =
                    type.arguments
                        .firstOrNull()
                        ?.type
                        ?.resolve()
                val elementTypeName = elementType?.declaration?.qualifiedName?.asString() ?: "Any"
                return "emptyArray<$elementTypeName>()"
            }

            return when (type.declaration.qualifiedName?.asString()) {
                "kotlin.String" -> "\"\""
                "kotlin.Int" -> "0"
                "kotlin.Long" -> "0L"
                "kotlin.Double" -> "0.0"
                "kotlin.Float" -> "0.0f"
                "kotlin.Boolean" -> "false"
                "kotlin.Byte" -> "0"
                "kotlin.Short" -> "0"
                "kotlin.Char" -> "'\\u0000'"
                else -> {
                    val typeName = type.declaration.qualifiedName?.asString() ?: type.toString()
                    val toLocationStr = assignmentDeclaration.to.locationString()

                    throw IllegalStateException(
                        "Cannot provide default value for unknown type '$typeName' at $toLocationStr.\n" +
                            "Supported default types are: String, Int, Long, Double, Float, Boolean, Byte, Short, Char, and Collections.",
                    )
                }
            }
        }
    }

    internal class MappedAssignment(
        assignmentDeclaration: AssignmentDeclaration,
        private val mapFunctionDeclaration: MapFunctionDeclaration,
        private val mapFunctionResolver: MapFunctionResolver,
        typeCheckHelper: CollectionTypeCheckHelper,
        private val matchingStrategy: MatchingStrategyType,
    ) : Assignment(assignmentDeclaration, typeCheckHelper) {
        override fun invoke(): String {
            val fromName = assignmentDeclaration.from.simpleName.asString()
            val toName = assignmentDeclaration.to.simpleName.asString()
            val fromType = assignmentDeclaration.from.type.resolve()
            val toType = assignmentDeclaration.to.type.resolve()

            // Check if both are enums - map by name using valueOf
            val fromDecl = fromType.declaration as? KSClassDeclaration
            val toDecl = toType.declaration as? KSClassDeclaration
            if (fromDecl?.classKind == ClassKind.ENUM_CLASS &&
                toDecl?.classKind == ClassKind.ENUM_CLASS
            ) {
                val fromEnumName = fromDecl.qualifiedName?.asString() ?: fromDecl.simpleName.asString()
                val toEnumName = toDecl.qualifiedName?.asString() ?: toDecl.simpleName.asString()
                val fromEnumEntries =
                    fromDecl.declarations
                        .filterIsInstance<KSClassDeclaration>()
                        .filter { it.classKind == ClassKind.ENUM_ENTRY }
                        .map { it.simpleName.asString() }
                val toEnumEntries =
                    toDecl.declarations
                        .filterIsInstance<KSClassDeclaration>()
                        .filter { it.classKind == ClassKind.ENUM_ENTRY }
                        .map { it.simpleName.asString() }

                val missingEnumEntries = fromEnumEntries.filterNot { it in toEnumEntries }
                if (missingEnumEntries.iterator().hasNext()) {
                    val fromLocationStr = assignmentDeclaration.from.locationString()
                    val toLocationStr = assignmentDeclaration.to.locationString()
                    throw IllegalStateException(
                        "Cannot map enum '$fromEnumName' to '$toEnumName' for property '$fromName' -> '$toName': " +
                            "target enum is missing entries ${missingEnumEntries.joinToString(", ")}.\n" +
                            "  Source: $fromLocationStr\n" +
                            "  Target: $toLocationStr",
                    )
                }

                val enumBranches =
                    fromEnumEntries.joinToString("\n") {
                        "$fromEnumName.$it -> $toEnumName.$it"
                    }
                val whenBranchesWith8Spaces = enumBranches.prependIndent("        ")
                val whenBranchesWith12Spaces = enumBranches.prependIndent("            ")

                val defaultEnumToUse =
                    toDecl.declarations
                        .filterIsInstance<KSClassDeclaration>()
                        .firstOrNull { it.classKind == ClassKind.ENUM_ENTRY }
                        ?.simpleName
                        ?.asString()

                return when {
                    fromType.isMarkedNullable && !toType.isMarkedNullable -> {
                        // Nullable to non-nullable - need default
                        if (defaultEnumToUse == null) {
                            val fromLocationStr = assignmentDeclaration.from.locationString()
                            val toLocationStr = assignmentDeclaration.to.locationString()

                            throw IllegalStateException(
                                "Cannot map nullable enum '$fromEnumName' to non-nullable enum '$toEnumName' " +
                                    "for property '$fromName' -> '$toName': target enum has no entries to use as default value.\n" +
                                    "  Source (nullable): $fromLocationStr\n" +
                                    "  Target (non-nullable): $toLocationStr",
                            )
                        }
                        val default = "$toEnumName.$defaultEnumToUse"
                        "$toName = when (it.$fromName) {\n        null -> $default\n$whenBranchesWith8Spaces\n    }"
                    }
                    fromType.isMarkedNullable -> {
                        "$toName = it.$fromName?.let { source ->\n        when (source) {\n$whenBranchesWith12Spaces\n        }\n    }"
                    }
                    else -> {
                        "$toName = when (it.$fromName) {\n$whenBranchesWith8Spaces\n    }"
                    }
                }
            }

            // Check if it's a collection - handle recursively
            if (isCollectionLike(fromType, typeCheckHelper) && isCollectionLike(toType, typeCheckHelper)) {
                val fromElementType =
                    fromType.arguments
                        .firstOrNull()
                        ?.type
                        ?.resolve()
                val toElementType =
                    toType.arguments
                        .firstOrNull()
                        ?.type
                        ?.resolve()

                if (fromElementType != null && toElementType != null) {
                    val accessExpr = if (fromType.isMarkedNullable) "it.$fromName?" else "it.$fromName"
                    val mappingExpression =
                        buildCollectionMappingExpression(
                            fromType,
                            toType,
                            accessExpr,
                            matchingStrategy,
                            mapFunctionResolver,
                            typeCheckHelper,
                        )

                    return if (fromType.isMarkedNullable && !toType.isMarkedNullable) {
                        "$toName = $mappingExpression ?: emptyList()"
                    } else {
                        "$toName = $mappingExpression"
                    }
                }
            }

            if (typeCheckHelper.isMap(fromType) && typeCheckHelper.isMap(toType)) {
                val mapExpression = "it.$fromName?.entries?.associate { it.key to it.value }"
                return if (fromType.isMarkedNullable && !toType.isMarkedNullable) {
                    "$toName = $mapExpression ?: emptyMap()"
                } else {
                    "$toName = $mapExpression"
                }
            }

            // Regular object mapping
            mapFunctionResolver.resolveRequiredMapFunction(mapFunctionDeclaration)
            val mapFnName =
                getMapFunctionName(
                    mapFunctionDeclaration.input,
                    mapFunctionDeclaration.output,
                )

            // If target is not nullable but source is, fail the build
            if (!toType.isMarkedNullable && fromType.isMarkedNullable) {
                val fromTypeName = fromType.declaration.qualifiedName?.asString() ?: fromType.toString()
                val toTypeName = toType.declaration.qualifiedName?.asString() ?: toType.toString()

                val fromLocationStr = assignmentDeclaration.from.locationString()
                val toLocationStr = assignmentDeclaration.to.locationString()

                throw IllegalStateException(
                    "Cannot map nullable type '$fromTypeName?' to non-nullable type '$toTypeName' " +
                        "for property '$fromName' -> '$toName'.\n" +
                        "  Source (nullable): $fromLocationStr\n" +
                        "  Target (non-nullable): $toLocationStr\n" +
                        "Either make the target property nullable or provide a default value.",
                )
            }

            return "$toName = $mapFnName(it.$fromName)"
        }
    }

    companion object {
        fun create(
            assignmentDeclaration: AssignmentDeclaration,
            mapFunctionResolver: MapFunctionResolver,
            typeCheckHelper: CollectionTypeCheckHelper,
            matchingStrategy: MatchingStrategyType,
        ): Assignment {
            val fromType = assignmentDeclaration.from.type.resolve()
            val toType = assignmentDeclaration.to.type.resolve()

            // FIRST: Check if it's a collection that needs recursive mapping
            // This must come before checking qualified names, because List<T> and List<U>
            // have the same qualified name but different element types
            if (isCollectionLike(fromType, typeCheckHelper) && isCollectionLike(toType, typeCheckHelper)) {
                val fromElementType =
                    fromType.arguments
                        .firstOrNull()
                        ?.type
                        ?.resolve()
                val toElementType =
                    toType.arguments
                        .firstOrNull()
                        ?.type
                        ?.resolve()

                if (fromElementType != null && toElementType != null) {
                    if (needsTypeMapping(fromElementType, toElementType, typeCheckHelper)) {
                        val nestedDeclaration =
                            MapFunctionDeclaration(fromType, toType, matchingStrategy)
                        mapFunctionResolver.resolveRequiredMapFunction(nestedDeclaration)
                        return MappedAssignment(
                            assignmentDeclaration,
                            nestedDeclaration,
                            mapFunctionResolver,
                            typeCheckHelper,
                            matchingStrategy,
                        )
                    }
                }
            }

            if (typeCheckHelper.isMap(fromType) && typeCheckHelper.isMap(toType)) {
                return DirectAssignment(assignmentDeclaration, typeCheckHelper)
            }

            // SECOND: Check if both are enums with the same simple name - map directly
            val fromDecl = fromType.declaration as? KSClassDeclaration
            val toDecl = toType.declaration as? KSClassDeclaration
            if (fromDecl?.classKind == ClassKind.ENUM_CLASS &&
                toDecl?.classKind == ClassKind.ENUM_CLASS
            ) {
                // If enum names match, map directly. Otherwise, they need manual mapping.
                if (fromDecl.simpleName.asString() == toDecl.simpleName.asString()) {
                    return DirectAssignment(assignmentDeclaration, typeCheckHelper)
                }
                // Different enum names - treat as different types, will use MappedAssignment
            }

            // THIRD: Check if types are compatible (same base type, possibly different nullability)
            val fromQualified = fromType.declaration.qualifiedName?.asString()
            val toQualified = toType.declaration.qualifiedName?.asString()

            return if (fromQualified == toQualified || toType.isAssignableFrom(fromType)) {
                DirectAssignment(assignmentDeclaration, typeCheckHelper)
            } else {
                val nestedDeclaration =
                    MapFunctionDeclaration(fromType, toType, matchingStrategy)
                mapFunctionResolver.resolveRequiredMapFunction(nestedDeclaration)
                MappedAssignment(
                    assignmentDeclaration,
                    nestedDeclaration,
                    mapFunctionResolver,
                    typeCheckHelper,
                    matchingStrategy,
                )
            }
        }
    }
}

/**
 * Assignment that uses a custom inline expression for transformation.
 * The expression is written directly into the generated code.
 */
internal class TransformedAssignment(
    private val declaration: TransformedAssignmentDeclaration,
    private val typeCheckHelper: CollectionTypeCheckHelper,
) : AssignmentGenerator {
    override fun invoke(): String {
        val toName = declaration.to.simpleName.asString()
        return "$toName = ${declaration.expression}"
    }
}
