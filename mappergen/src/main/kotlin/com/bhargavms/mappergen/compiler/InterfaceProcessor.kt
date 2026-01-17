package com.bhargavms.mappergen.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration

class InterfaceProcessor(
    private val methodProcessor: MethodProcessor,
) {
    fun process(ksClassDeclaration: KSClassDeclaration) {
        ksClassDeclaration.getAllFunctions().forEach {
            methodProcessor.process(it)
        }
    }
}
