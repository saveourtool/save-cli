/**
 * This file contains code for codegen: generating a list of options for config files and README.
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.generation

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.Project

import java.io.BufferedReader
import java.io.File

/**
 * The comment that will be added to the generated sources file.
 */
private val autoGenerationComment =
        """
            | ---------------------------------------------------------------------
            | ******* This file was auto generated, please don't modify it. *******
            | ---------------------------------------------------------------------
        """.trimMargin()

/**
 * This class represents the general form of each key in json file with config options
 * @property argType Type which will be used by ArgParser
 * @property kotlinType Type which will be used in kotlin code
 * @property fullName Full name of option for usage in Save cli
 * @property shortName Short name of option for usage in Save cli
 * @property description Option description
 * @property default default value for option
 */
@Suppress(
    "USE_DATA_CLASS",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "KDOC_NO_CLASS_BODY_PROPERTIES_IN_HEADER",
    "WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES"
)
class Option {
    lateinit var argType: String
    lateinit var kotlinType: String
    lateinit var fullName: String
    lateinit var shortName: String
    lateinit var description: String
    var default: String? = null
}

/**
 * This class represents the general form of each key in json file with config options
 * @property argType Type which will be used by ArgParser
 * @property kotlinType Type which will be used in kotlin code
 * @property fullName Full name of option for usage in Save cli
 * @property shortName Short name of option for usage in Save cli
 * @property description Option description
 * @property default default value for option
 */
@Suppress(
    "USE_DATA_CLASS",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "KDOC_NO_CLASS_BODY_PROPERTIES_IN_HEADER",
    "WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES"
)
class Argument {
    lateinit var argType: String
    lateinit var kotlinType: String
    lateinit var description: String
    var default: String? = null
    var vararg: Boolean = false
}

/**
 * Paths, where to store generated files
 */
fun Project.generatedOptionsTablePath() = "$rootDir/OptionsTable.md"

/**
 * Path to file with options config
 */
fun Project.optionsConfigFilePath() = "$rootDir/buildSrc/src/main/resources/config-options.json"

/**
 * Path to file with arguments config
 */
fun Project.argumentsConfigFilePath() = "$rootDir/buildSrc/src/main/resources/config-arguments.json"

/**
 * Generate options for ArgParser
 *
 * @param jsonObject map of cli option names to [Option] objects
 * @return a corresponding [FunSpec.Builder]
 */
@Suppress("TOO_MANY_LINES_IN_LAMBDA")
fun FunSpec.Builder.generateOptions(jsonObject: Map<String, Option>): FunSpec.Builder {
    jsonObject.forEach { (key, value) ->
        this.addCode(
            CodeBlock.builder()
                .add("val %N = parser.option(\n", key + "Option")
                .indent()
                .add("${value.argType},\n")
                .add("fullName = %S,\n", value.fullName)
                .also {
                    if (value.shortName.isNotEmpty()) {
                        it.add("shortName = %S,\n", value.shortName)
                    }
                }
                // We replace whitespaces to `·`, in aim to avoid incorrect line breaking,
                // which could be done by kotlinpoet (see https://github.com/square/kotlinpoet/issues/598)
                .add("description = %S\n", value.description.replace(" ", "·"))
                .unindent()
                .add(")\n")
                .also {
                    if (value.default != null) {
                        it.add(".%M(${stringOrLiteral(value.kotlinType)})\n",
                            MemberName("kotlinx.cli", "default"),
                            value.default)
                    }
                }
                .addStatement("val %N by %N", key, key + "Option")
                .build()
        )
    }
    return this
}

/**
 * Generate arguments for ArgParser
 *
 * @param jsonObject map of cli option names to [Argument] objects
 * @return a corresponding [FunSpec.Builder]
 */
@Suppress("TOO_MANY_LINES_IN_LAMBDA")
fun FunSpec.Builder.generateAgruments(jsonObject: Map<String, Argument>): FunSpec.Builder {
    jsonObject.forEach { (key, value) ->
        this.addCode(CodeBlock.builder()
            .add("val %N = parser.argument(\n", key + "Argument")
            .indent()
            .add("${value.argType},\n")
            // We replace whitespaces to `·`, in aim to avoid incorrect line breaking,
            // which could be done by kotlinpoet (see https://github.com/square/kotlinpoet/issues/598)
            .add("description = %S\n", value.description.replace(" ", "·"))
            .unindent()
            .add(")\n")
            .also {
                if (value.default == null && value.vararg) {
                    it.add(".%M()\n", MemberName("kotlinx.cli", "optional"))
                }
            }
            .also {
                if (value.vararg) {
                    it.add(".%M()\n", MemberName("kotlinx.cli", "vararg"))
                }
            }
            .also {
                if (value.default != null) {
                    it.add(".%M(%L)\n",
                        MemberName("kotlinx.cli", "default"),
                        value.default)
                }
            }
            .addStatement("val %N by %N", key, key + "Argument")
            .build())
    }
    return this
}

/**
 * Assign class members to options
 *
 * @param options map of cli option names to [Option] objects
 * @param arguments map of cli argument names to [Argument] objects
 * @return a corresponding [FunSpec.Builder]
 */
fun FunSpec.Builder.assignMembers(options: Map<String, Option>, arguments: Map<String, Argument>): FunSpec.Builder {
    val cliUtilsClassName = ClassName.bestGuess("com.saveourtool.save.core.utils.CliUtils")
    this.addCode(CodeBlock.builder().indent().build())
    options.forEach { (key, _) ->
        this.addCode(CodeBlock.builder()
            .add("%N = %T.resolveValue(%N, %N, %N.%N),", key, cliUtilsClassName, key + "Option", key, "propertiesFromFileOrDefault", key)
            .add("\n")
            .build())
    }
    arguments.forEach { (key, value) ->
        if (value.default != null) {
            this.addCode(CodeBlock.builder()
                .add("%N = %T.resolveValue(%N, %N, %N.%N),", key, cliUtilsClassName, key + "Argument", key, "propertiesFromFileOrDefault", key)
                .add("\n")
                .build())
        } else {
            this.addCode(CodeBlock.builder()
                .add("%N = %N,", key, key)
                .add("\n")
                .build())
        }
    }
    return this
}

/**
 * General function for auto generation of config options and readme table
 *
 * @param destination a destination file for generated code
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun Project.generateConfigOptions(destination: File) {
    val options: Map<String, Option> = readConfig(optionsConfigFilePath())
    val arguments: Map<String, Argument> = readConfig(argumentsConfigFilePath())
    generateSaveProperties(options, arguments, destination)
    generateReadme(options, File(generatedOptionsTablePath()))
}

/**
 * Generate SaveProperties class which represents configuration properties of SAVE application
 *
 * @param options map of cli option names to [Option] objects
 * @param arguments map of cli argument names to [Argument] objects
 * @param destination
 */
fun generateSaveProperties(
    options: Map<String, Option>,
    arguments: Map<String, Argument>,
    destination: File
) {
    val builder = FileSpec.builder("com.saveourtool.save.core.config", "SaveProperties")
    builder.addFileComment(autoGenerationComment)
    builder.addImport("kotlinx.cli", "ArgParser")
    builder.addImport("kotlinx.cli", "ArgType")
    val classBuilder = generateSavePropertiesClass(options, arguments)
    val companion = TypeSpec.companionObjectBuilder()
        .addFunction(generateParseArgsFunc(options, arguments).build())
        .build()
    classBuilder.addType(companion)
    builder.addType(classBuilder.build())
    builder.addType(genereatePropertiesClassWithDefaults("SaveProperties", options, arguments).build())

    builder.indent("    ")
    destination.writeText(builder.build().toString())
}

/**
 * Generate constructors for SaveProperties class
 *
 * @param options map of cli option names to [Option] objects
 * @param arguments map of cli argument names to [Argument] objects
 * @return a corresponding [TypeSpec.Builder]
 */
@Suppress("TOO_LONG_FUNCTION")
fun generateSavePropertiesClass(options: Map<String, Option>, arguments: Map<String, Argument>): TypeSpec.Builder {
    val classBuilder = TypeSpec.classBuilder("SaveProperties").addModifiers(KModifier.DATA)
    val optionToDescription = options.map { (key, value) -> key to value.description }
    val argumentToDescription = arguments.map { (key, value) -> key to value.description }
    val properties = (optionToDescription + argumentToDescription).joinToString("\n") { (key, description) ->
        "@property $key $description"
    }
    val kdoc = """
               |Configuration properties of save application, retrieved either from properties file
               |or from CLI args.
               |$properties
               """.trimMargin()
    classBuilder.addKdoc(kdoc)

    // Generate primary ctor
    val primaryCtor = FunSpec.constructorBuilder()
    for ((name, value) in options) {
        var propertyClassName = createClassName(value.kotlinType).copy(nullable = value.default == null)
        primaryCtor.addParameter(ParameterSpec.builder(name, propertyClassName)
            .defaultValue(stringOrLiteral(value.kotlinType), value.default)
            .build())
        val property = PropertySpec.builder(name, propertyClassName)
            .initializer(name)
        classBuilder.addProperty(property.build())
    }
    for ((name, value) in arguments) {
        var propertyClassName = createClassName(value.kotlinType)
        primaryCtor.addParameter(ParameterSpec.builder(name, propertyClassName)
            .let {
                val default = value.default
                if (default != null) {
                    it.defaultValue(default)
                } else if (value.vararg) {
                    it.defaultValue("%M()", MemberName("kotlin.collections", "emptyList"))
                } else {
                    it
                }
            }
            .build())
        val property = PropertySpec.builder(name, propertyClassName)
            .initializer(name)
        classBuilder.addProperty(property.build())
    }
    classBuilder.primaryConstructor(primaryCtor.build())
    return classBuilder
}

/**
 * @param className class name of generating class
 * @param options map of cli option names to [Option] objects
 * @param arguments map of cli argument names to [Argument] objects
 * @return returns a data class which contains only properties are settable via properties file
 */
fun genereatePropertiesClassWithDefaults(
    className: String,
    options: Map<String, Option>,
    arguments: Map<String, Argument>
): TypeSpec.Builder {
    val classBuilder = TypeSpec.classBuilder("${className}Defaults").addModifiers(KModifier.DATA, KModifier.PRIVATE)
    classBuilder.addAnnotation(AnnotationSpec.builder(ClassName("kotlinx.serialization", "Serializable")).build())

    // Generate primary ctor
    val primaryCtor = FunSpec.constructorBuilder()
    options.forEach { (name, value) ->
        var propertyClassName = createClassName(value.kotlinType).copy(nullable = value.default == null)
        primaryCtor.addParameter(ParameterSpec.builder(name, propertyClassName)
            .defaultValue(stringOrLiteral(value.kotlinType), value.default)
            .build())
        val property = PropertySpec.builder(name, propertyClassName)
            .initializer(name)
        classBuilder.addProperty(property.build())
    }
    arguments.forEach { (name, value) ->
        val default = value.default
        if (default != null) {
            var propertyClassName = createClassName(value.kotlinType)
            primaryCtor.addParameter(ParameterSpec.builder(name, propertyClassName)
                .defaultValue(default)
                .build())
            val property = PropertySpec.builder(name, propertyClassName)
                .initializer(name)
            classBuilder.addProperty(property.build())
        }
    }
    classBuilder.primaryConstructor(primaryCtor.build())
    return classBuilder
}

/**
 * Create ClassName object from string, which represents generic kotlin type
 *
 * @param type kotlin type
 * @return corresponding ClassName object
 */
// TODO: For now generic types with multiple args (like Map) doesn't supported
fun createClassName(type: String): TypeName {
    if (!type.contains("<")) {
        return extractClassNameFromString(type)
    }
    val packageName = type.substringBefore("<")
    val simpleName = type.substringAfter("<").substringBeforeLast(">")
    return extractClassNameFromString(packageName).parameterizedBy(createClassName(simpleName))
}

/**
 * Create ClassName object from string, which represents simple kotlin type
 *
 * @param type kotlin type
 * @return corresponding ClassName object
 */
fun extractClassNameFromString(type: String) = ClassName(type.substringBeforeLast("."), type.substringAfterLast("."))

/**
 * Generate function to generate parseAgrsFunc()
 *
 * @param options map of cli option names to [Option] objects
 * @param arguments map of cli argument names to [Argument] objects
 * @return a corresponding [FunSpec.Builder]
 */
fun generateParseArgsFunc(options: Map<String, Option>, arguments: Map<String, Argument>): FunSpec.Builder {
    val parseArgsFunc = FunSpec.builder("parseArgs")
    parseArgsFunc.returns(ClassName("com.saveourtool.save.core.config", "SaveProperties"))
    parseArgsFunc.addParameter("args", ClassName("kotlin", "Array")
        .parameterizedBy(ClassName("kotlin", "String")))
    parseArgsFunc.addStatement("val parser = ArgParser(\"save\")")
        .generateOptions(options)
        .generateAgruments(arguments)
        .addStatement("parser.parse(args)")
        .addStatement("val propertiesFromFileOrDefault = %T.parsePropertiesFile<%T>(%N, %S)",
            ClassName.bestGuess("com.saveourtool.save.core.utils.CliUtils"),
            ClassName("com.saveourtool.save.core.config", "SavePropertiesDefaults"),
            "testRootDir",
            "save")
        .addStatement("return %T(", ClassName("com.saveourtool.save.core.config", "SaveProperties"))
        .assignMembers(options, arguments)
        .addStatement(")")
    return parseArgsFunc
}

/**
 * Generate readme table from json object
 *
 * @param jsonObject map of cli option names to [Option] objects
 * @param destination a destination file to write the table into
 */
@Suppress("TOO_MANY_LINES_IN_LAMBDA")
fun generateReadme(jsonObject: Map<String, Option>, destination: File) {
    var readmeContent =
            """
                |Most (except for `-h` and `-prop`) of the options below can be passed to a SAVE via `save.properties` file
                |
                || Short name | Long name  | Description   | Default |
                ||------------|------------|---------------|---------------|
                || h | help | Usage info | - |
            """.trimMargin()
    jsonObject.forEach {
        val shortName = if (it.value.shortName.isNotEmpty()) it.value.shortName else "-"
        val longName = it.value.fullName
        val description = it.value.description
        var default = it.value.default
        // If some option have user defined type, then we will print to the README
        // only the value (e.g. LanguageType.UNDEFINED --> UNDEFINED)
        if (default != null) {
            if (it.value.kotlinType != "kotlin.String") {
                default = default.substringAfterLast(".")
            }
        } else {
            default = "-"
        }
        readmeContent += "\n| $shortName | $longName | $description | $default |"
    }
    destination.writeText(readmeContent)
}

/**
 * Read config from provided file
 *
 * @param configFile a path to file
 * @return map which is parsed from file [confinFile]
 */
private inline fun <reified T> readConfig(configFile: String): Map<String, T> {
    val gson = Gson()
    val bufferedReader: BufferedReader = File(configFile).bufferedReader()
    val jsonString = bufferedReader.use { it.readText() }
    return gson.fromJson(jsonString, object : TypeToken<Map<String, T>>() {}.type)
}

private fun stringOrLiteral(kotlinType: String): String = "%${if (kotlinType.contains("String")) "S" else "L"}"
