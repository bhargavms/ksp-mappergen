package com.bhargavms.mappergen.testsupport

import com.bhargavms.mappergen.compiler.MapperGenProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.useKsp2
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
fun compileWithMapperGen(vararg sources: SourceFile): KotlinCompilation =
    KotlinCompilation().apply {
        inheritClassPath = true
        this.sources = sources.toList()
        useKsp2()
        configureKsp {
            symbolProcessorProviders.add(MapperGenProcessorProvider())
        }
    }

@OptIn(ExperimentalCompilerApi::class)
fun compileWithMapperGenFromResources(vararg resourcePaths: String): KotlinCompilation {
    val classpathSources =
        resourcePaths.map { path ->
            val content =
                MapperGenTestSupport::class.java.classLoader
                    .getResource(path)
                    ?.readText()
                    ?: error("Missing test resource: $path")
            val fileName = path.substringAfterLast('/')
            SourceFile.kotlin(fileName, content)
        }
    return compileWithMapperGen(*classpathSources.toTypedArray())
}

object MapperGenTestSupport {
    fun fixture(path: String): String =
        javaClass.classLoader
            .getResource(path)
            ?.readText()
            ?.trim()
            ?: error("Missing fixture: $path")

    @OptIn(ExperimentalCompilerApi::class)
    fun generatedFile(
        compilation: KotlinCompilation,
        relativePath: String,
    ): String {
        val fileName = relativePath.substringAfterLast('/')
        val file =
            compilation.kspSourcesDir
                .walkTopDown()
                .firstOrNull { it.isFile && it.name == fileName }
                ?: run {
                    val found =
                        compilation.kspSourcesDir
                            .walkTopDown()
                            .filter { it.isFile }
                            .joinToString("\n") { it.relativeTo(compilation.kspSourcesDir).path }
                    error("Generated file not found: $relativePath under ${compilation.kspSourcesDir}\nFound:\n$found")
                }
        return file.readText().trim()
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun assertGeneratedMatchesFixture(
        compilation: KotlinCompilation,
        generatedRelativePath: String,
        fixtureResourcePath: String,
    ) {
        val expected = fixture(fixtureResourcePath)
        val actual = generatedFile(compilation, generatedRelativePath)
        org.junit.jupiter.api.Assertions
            .assertEquals(expected, actual, "Generated content mismatch for $generatedRelativePath")
    }
}
