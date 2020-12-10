package com.bhargavms.mappergen.compiler.errors

import kotlin.reflect.KClass

class BadAnnotationTargetException(
    annotation: KClass<out Annotation>,
    targetType: String
) : Exception("${annotation.simpleName} can only be appied to $targetType")