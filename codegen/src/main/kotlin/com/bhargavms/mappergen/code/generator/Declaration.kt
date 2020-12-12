package com.bhargavms.mappergen.code.generator

import com.bhargavms.mappergen.code.generator.utils.typeName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

sealed class Declaration

data class MapFunctionDeclaration(
    val input: KSType,
    val output: KSType
) : Declaration()

internal data class AssignmentDeclaration(
    val from: KSPropertyDeclaration,
    val to: KSPropertyDeclaration
) : Declaration()

internal abstract class MapFunction(
    protected val mapFunctionDeclaration: MapFunctionDeclaration,
    private val mapFunctionResolver: MapFunctionResolver
) {
    abstract fun generateFunction(): FunSpec

    protected val mapFunctionName: String by lazy {
        getMapFunctionName(
            mapFunctionDeclaration.input,
            mapFunctionDeclaration.output
        )
    }

    protected val assignments: List<Assignment> =
        (mapFunctionDeclaration.output.declaration as KSClassDeclaration).getAllProperties()
            .mapNotNull { outputProperty ->
                (mapFunctionDeclaration.input.declaration as KSClassDeclaration)
                    .findMostSimilarField(
                        outputProperty.simpleName.asString()
                    )?.let { similarInputProperty ->
                        AssignmentDeclaration(from = similarInputProperty, to = outputProperty)
                    }
            }.toList().map {
                Assignment.create(it, mapFunctionResolver)
            }


    private fun KSClassDeclaration.findMostSimilarField(forName: CharSequence): KSPropertyDeclaration? {
        var mostSimilarField: Pair<KSPropertyDeclaration, Int>? = null
        this.getAllProperties().forEach {
            val bigger: CharSequence
            val smaller: CharSequence
            if (forName.length > it.simpleName.asString().length) {
                bigger = forName
                smaller = it.simpleName.asString()
            } else {
                bigger = it.simpleName.asString()
                smaller = forName
            }
            val similarChars = getSimilarCharsInSequence(bigger, smaller)
            if (similarChars > (mostSimilarField?.second ?: -1)) {
                mostSimilarField = Pair(it, similarChars)
            }
        }
        return mostSimilarField?.first
    }

    private fun getSimilarCharsInSequence(bigger: CharSequence, smaller: CharSequence): Int {
        return if (bigger.contains(smaller) || smaller.length - 2 < 0) {
            smaller.length
        } else {
            getSimilarCharsInSequence(bigger, smaller.subSequence(0, smaller.length - 2))
        }
    }

    companion object {
        fun create(
            mapFunctionDeclaration: MapFunctionDeclaration,
            mapFunctionResolver: MapFunctionResolver,
            typeCheckHelper: CollectionTypeCheckHelper
        ): MapFunction {
            mapFunctionDeclaration.output.isMarkedNullable
            return if (typeCheckHelper.isArray(mapFunctionDeclaration.output) ||
                typeCheckHelper.isIterable(mapFunctionDeclaration.output)
            ) {
                IterableObjectMapFunction(mapFunctionDeclaration, mapFunctionResolver)
            } else {
                PlainObjectMapFunction(mapFunctionDeclaration, mapFunctionResolver)
            }
        }
    }
}

internal class IterableObjectMapFunction(
    mapFunctionDeclaration: MapFunctionDeclaration,
    mapFunctionResolver: MapFunctionResolver
) : MapFunction(mapFunctionDeclaration, mapFunctionResolver) {
    override fun generateFunction(): FunSpec {
        return FunSpec.builder(mapFunctionName)
            .addParameter("input", mapFunctionDeclaration.input.typeName().copy(true))
            .addCode("return input?.mapNotNull {")
            .apply {
                assignments.forEach {
                    addCode(it.generateStatement())
                }
            }
            .addCode("}")
            .build()
    }
}

internal class PlainObjectMapFunction(
    mapFunctionDeclaration: MapFunctionDeclaration,
    mapFunctionResolver: MapFunctionResolver
) : MapFunction(mapFunctionDeclaration, mapFunctionResolver) {
    override fun generateFunction(): FunSpec {
        return FunSpec.builder(mapFunctionName)
            .addParameter("input", mapFunctionDeclaration.input.typeName().copy(true))
            .returns(mapFunctionDeclaration.output.typeName().copy(true))
            .addCode("return input?.run {\n")
            .apply {
                assignments.forEach {
                    addCode(it.generateStatement())
                }
            }
            .addCode("}")
            .build()
    }
}

fun getMapFunctionName(fromType: KSType, toType: KSType): String =
    "map${getName(fromType)}To${getName(toType)}"

fun getName(type: KSType): String {
    val builder = StringBuilder()
    type.arguments.forEach {
        it.type?.let { builder.append(getName(it.resolve())) }
    }
    builder.append(type.declaration.simpleName.asString())
    return builder.toString()
}

internal sealed class Assignment(protected val assignmentDeclaration: AssignmentDeclaration) {
    abstract fun generateStatement(): CodeBlock
    internal class DirectAssignment(
        assignmentDeclaration: AssignmentDeclaration
    ) : Assignment(assignmentDeclaration) {
        override fun generateStatement(): CodeBlock {
            return CodeBlock.builder().addStatement(
                "${assignmentDeclaration.to.simpleName.asString()} = " +
                        assignmentDeclaration.from.simpleName.asString()
            ).build()
        }
    }

    internal class MappedAssignment(
        assignmentDeclaration: AssignmentDeclaration,
        private val mapFunctionDeclaration: MapFunctionDeclaration,
        private val mapFunctionResolver: MapFunctionResolver
    ) : Assignment(assignmentDeclaration) {

        override fun generateStatement(): CodeBlock {
            mapFunctionResolver.resolveRequiredMapFunction(mapFunctionDeclaration)
            val fromType = mapFunctionDeclaration.input
            val toType = mapFunctionDeclaration.output
            return CodeBlock.builder()
                .addStatement(
                    "${assignmentDeclaration.to.simpleName.asString()} = " +
                            getMapFunctionName(fromType, toType)
                )
                .build()
        }
    }

    companion object {
        fun create(
            assignmentDeclaration: AssignmentDeclaration,
            mapFunctionResolver: MapFunctionResolver
        ): Assignment {
            val fromType = assignmentDeclaration.from.type.resolve()
            val toType = assignmentDeclaration.to.type.resolve()
            return if (toType.isAssignableFrom(fromType)) {
                DirectAssignment(assignmentDeclaration)
            } else {
                MappedAssignment(
                    assignmentDeclaration, MapFunctionDeclaration(fromType, toType),
                    mapFunctionResolver
                )
            }
        }
    }
}

