package com.bhargavms.mappergen.code.generator

import com.bhargavms.mappergen.code.generator.utils.typeName
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.NonExistLocation
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

sealed class Declaration

data class MapFunctionDeclaration(
    val input: KSType,
    val output: KSType,
) : Declaration()

internal data class AssignmentDeclaration(
    val from: KSPropertyDeclaration,
    val to: KSPropertyDeclaration,
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

    protected val assignments: List<Assignment> =
        (mapFunctionDeclaration.output.declaration as KSClassDeclaration)
            .getAllProperties()
            .mapNotNull { outputProperty ->
                (mapFunctionDeclaration.input.declaration as KSClassDeclaration)
                    .findMatchingField(outputProperty.simpleName.asString())
                    ?.let { matchingInputProperty ->
                        AssignmentDeclaration(from = matchingInputProperty, to = outputProperty)
                    }
            }.toList()
            .map {
                Assignment.create(it, mapFunctionResolver, typeCheckHelper)
            }

    /**
     * Find a field with exact name match (case-insensitive)
     */
    private fun KSClassDeclaration.findMatchingField(forName: String): KSPropertyDeclaration? =
        getAllProperties().firstOrNull {
            it.simpleName.asString().equals(forName, ignoreCase = true)
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

        val constructorArgs =
            assignments.mapIndexed { index, assignment ->
                val separator = if (index < assignments.size - 1) "," else ""
                assignment.generateConstructorArg() + separator
            }

        return FunSpec
            .builder(mapFunctionName)
            .addParameter("input", mapFunctionDeclaration.input.typeName().copy(true))
            .returns(outputTypeName.copy(nullable = true))
            .apply {
                addCode("return input?.let {\n")
                addCode("    %T(\n", outputTypeName)
                constructorArgs.forEach { arg ->
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

internal sealed class Assignment(
    protected val assignmentDeclaration: AssignmentDeclaration,
    protected val typeCheckHelper: CollectionTypeCheckHelper,
) {
    abstract fun generateStatement(): CodeBlock

    abstract fun generateConstructorArg(): String

    internal class DirectAssignment(
        assignmentDeclaration: AssignmentDeclaration,
        typeCheckHelper: CollectionTypeCheckHelper,
    ) : Assignment(assignmentDeclaration, typeCheckHelper) {
        override fun generateStatement(): CodeBlock {
            val fromName = assignmentDeclaration.from.simpleName.asString()
            val toName = assignmentDeclaration.to.simpleName.asString()
            return CodeBlock
                .builder()
                .addStatement("$toName = it.$fromName")
                .build()
        }

        override fun generateConstructorArg(): String {
            val fromName = assignmentDeclaration.from.simpleName.asString()
            val toName = assignmentDeclaration.to.simpleName.asString()
            val fromType = assignmentDeclaration.from.type.resolve()
            val toType = assignmentDeclaration.to.type.resolve()

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
    ) : Assignment(assignmentDeclaration, typeCheckHelper) {
        override fun generateStatement(): CodeBlock {
            mapFunctionResolver.resolveRequiredMapFunction(mapFunctionDeclaration)
            val fromName = assignmentDeclaration.from.simpleName.asString()
            val toName = assignmentDeclaration.to.simpleName.asString()
            val mapFnName =
                getMapFunctionName(
                    mapFunctionDeclaration.input,
                    mapFunctionDeclaration.output,
                )
            return CodeBlock
                .builder()
                .addStatement("$toName = $mapFnName(it.$fromName)")
                .build()
        }

        override fun generateConstructorArg(): String {
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
                        .lastOrNull { it.classKind == ClassKind.ENUM_ENTRY }
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
            val fromQualifiedName = fromType.declaration.qualifiedName?.asString() ?: ""
            val toQualifiedName = toType.declaration.qualifiedName?.asString() ?: ""
            val isFromCollection =
                typeCheckHelper.isIterable(fromType) ||
                    typeCheckHelper.isArray(fromType) ||
                    fromQualifiedName.startsWith("kotlin.collections.")
            val isToCollection =
                typeCheckHelper.isIterable(toType) ||
                    typeCheckHelper.isArray(toType) ||
                    toQualifiedName.startsWith("kotlin.collections.")

            if (isFromCollection && isToCollection) {
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
                    val elementMapFnName = getMapFunctionName(fromElementType, toElementType)
                    mapFunctionResolver.resolveRequiredMapFunction(
                        MapFunctionDeclaration(fromElementType, toElementType),
                    )

                    return if (fromType.isMarkedNullable && !toType.isMarkedNullable) {
                        "$toName = it.$fromName?.mapNotNull { $elementMapFnName(it) } ?: emptyList()"
                    } else if (fromType.isMarkedNullable) {
                        "$toName = it.$fromName?.mapNotNull { $elementMapFnName(it) }"
                    } else {
                        "$toName = it.$fromName.mapNotNull { $elementMapFnName(it) }"
                    }
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
        ): Assignment {
            val fromType = assignmentDeclaration.from.type.resolve()
            val toType = assignmentDeclaration.to.type.resolve()

            // FIRST: Check if it's a collection that needs recursive mapping
            // This must come before checking qualified names, because List<T> and List<U>
            // have the same qualified name but different element types
            val fromQualifiedName = fromType.declaration.qualifiedName?.asString() ?: ""
            val toQualifiedName = toType.declaration.qualifiedName?.asString() ?: ""
            val isFromCollection =
                typeCheckHelper.isIterable(fromType) ||
                    typeCheckHelper.isArray(fromType) ||
                    fromQualifiedName.startsWith("kotlin.collections.")
            val isToCollection =
                typeCheckHelper.isIterable(toType) ||
                    typeCheckHelper.isArray(toType) ||
                    toQualifiedName.startsWith("kotlin.collections.")

            if (isFromCollection && isToCollection) {
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
                    // Check if element types are the same (direct assignment) or need mapping
                    val fromElementQualified = fromElementType.declaration.qualifiedName?.asString()
                    val toElementQualified = toElementType.declaration.qualifiedName?.asString()

                    // If element types are different, we need recursive mapping
                    if (fromElementQualified != toElementQualified) {
                        // Elements need mapping - use MappedAssignment which handles collections
                        return MappedAssignment(
                            assignmentDeclaration,
                            MapFunctionDeclaration(fromType, toType),
                            mapFunctionResolver,
                            typeCheckHelper,
                        )
                    }
                    // If element types are the same, fall through to direct assignment
                }
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
                MappedAssignment(
                    assignmentDeclaration,
                    MapFunctionDeclaration(fromType, toType),
                    mapFunctionResolver,
                    typeCheckHelper,
                )
            }
        }
    }
}
