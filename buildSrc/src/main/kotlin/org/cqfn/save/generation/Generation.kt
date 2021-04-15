/**
 * This file contains code for codegen: generating a list of options for config files and README.
 */

package org.cqfn.save.generation

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeName

import java.io.File
import java.io.BufferedReader

/**
 * The comment that will be added to the generated sources file.
 */
private val autoGenerationComment =
    """
    | ---------------------------------------------------------------------
    | ******* This file was auto generated, please don't modify it. *******
    | ---------------------------------------------------------------------
    """.trimMargin()

// Path to config file
val configFilePath = "buildSrc/src/main/resources/config-options.json"

// Paths, where to store generated files
val generatedSaveSavePropertiesPath = "save-core/src/commonMain/kotlin/org/cqfn/save/core/config/"
val generatedOptionsTablePath = "."

/**
 * This class represents the general form of each key in json file with config options
 * @property argType Type which will be used by ArgParser
 * @property kotlinType Type which will be used in kotlin code
 * @property fullName Full name of option for usage in Save cli
 * @property shortName Short name of option for usage in Save cli
 * @property description Option description
 * @property default default value for option
 */
class Option {
    lateinit var argType: String
    lateinit var kotlinType: String
    lateinit var fullName: String
    lateinit var shortName: String
    lateinit var description: String
    lateinit var default: String
}

fun generateConfigOptions() {
    val configFile = configFilePath
    val gson = Gson()
    val bufferedReader: BufferedReader = File(configFile).bufferedReader()
    val jsonString = bufferedReader.use { it.readText() }
    val jsonObject = gson.fromJson<Map<String, Option>>(jsonString, object : TypeToken<Map<String, Option>>(){}.type)
    generateSaveProperties(jsonObject)
    generateReadme(jsonObject)
}

fun generateSaveProperties(jsonObject: Map<String, Option>) {
    val builder = FileSpec.builder("org.cqfn.save.core.config", "SaveProperties")
    builder.addComment(autoGenerationComment)
    builder.addImport("kotlinx.cli", "ArgParser")
    builder.addImport("kotlinx.cli", "ArgType")
    val classBuilder = generateSavePropertiesClass(jsonObject)
    val mergeFunc = generateMergeConfigFunc(jsonObject)
    classBuilder.addFunction(mergeFunc.build())
    builder.addType(classBuilder.build()).indent("    ")
    File("$generatedSaveSavePropertiesPath/SaveProperties.kt").writeText(builder.build().toString())
}

fun generateSavePropertiesClass(jsonObject: Map<String, Option>): TypeSpec.Builder {
    val classBuilder = TypeSpec.classBuilder("SaveProperties")
    val properties = jsonObject.entries.joinToString("\n") { "@property ${it.key} ${it.value.description}" }
    val kdoc = """
               |Configuration properties of save application, retrieved either from properties file
               |or from CLI args.
               |$properties
               """.trimMargin()
    classBuilder.addKdoc(kdoc)
    classBuilder.addAnnotation(AnnotationSpec.builder(ClassName("kotlinx.serialization", "Serializable")).build())
    // Generate primary ctor
    val primaryCtor = FunSpec.constructorBuilder()
    for ((name, value) in jsonObject) {
        primaryCtor.addParameter(ParameterSpec.builder(name, createClassName(value.kotlinType).copy(nullable = true))
            .defaultValue(run {
                var default = value.default
                if (default != "null" && value.kotlinType == "kotlin.String") {
                    default = "\"$default\""
                }
                default
            })
            .build())
        val property = PropertySpec.builder(name, createClassName(value.kotlinType).copy(nullable = true))
            .initializer(name)
            .mutable()
        classBuilder.addProperty(property.build())
    }
    classBuilder.primaryConstructor(primaryCtor.build())
    // Generate secondary ctor
    val secondaryCtor = FunSpec.constructorBuilder()
    secondaryCtor.addParameter("args", ClassName("kotlin", "Array")
                                           .parameterizedBy(ClassName("kotlin", "String")))
    secondaryCtor.callThisConstructor()
    secondaryCtor.addStatement("val parser = ArgParser(\"save\")")
                    .generateOptions(jsonObject)
                        .addStatement("parser.parse(args)")
    classBuilder.addFunction(secondaryCtor.build())
    return classBuilder
}

// Generate options for ArgParser
fun FunSpec.Builder.generateOptions(jsonObject: Map<String, Option>): FunSpec.Builder {
    jsonObject.forEach {
        var option = "${it.key} = parser.option(\n"
        option += "${it.value.argType},\n"
        option += "fullName = \"${it.value.fullName}\",\n"
        if (it.value.shortName.isNotEmpty()) {
            option += "shortName = \"${it.value.shortName}\",\n"
        }
        // We replace whitespaces to `·`, in aim to avoid incorrect line breaking,
        // which could be done by kotlinpoet
        option += "description = \"${it.value.description.replace(" ", "·")}\"\n"
        option += ").value\n"
        this.addStatement(option)
    }
    return this
}
// TODO: For now generic types with multiple args (like Map) doesn't supported
fun createClassName(type: String): TypeName {
    if (!type.contains("<")) {
        return extractClassNameFromString(type)
    }
    val packageName = type.substringBefore("<")
    val simpleName = type.substringAfter("<").substringBeforeLast(">")
    return extractClassNameFromString(packageName).parameterizedBy(createClassName(simpleName))
}

fun extractClassNameFromString(type: String): ClassName {
    return ClassName(type.substringBeforeLast("."), type.substringAfterLast("."))
}

fun generateMergeConfigFunc(jsonObject: Map<String, Option>): FunSpec.Builder {
    val kdoc =
        """ 
            |@param configFromPropertiesFile - config that will be used as a fallback in case when the field was not provided
            |@return this configuration
            """.trimMargin()
    val mergeFunc = FunSpec.builder("mergeConfigWithPriorityToThis")
                        .addKdoc(kdoc)
                            .addParameter("configFromPropertiesFile", ClassName("org.cqfn.save.core.config", "SaveProperties"))
                                .returns(ClassName("org.cqfn.save.core.config", "SaveProperties"))
    val statements = jsonObject.entries.joinToString("\n") { "${it.key} = ${it.key} ?: configFromPropertiesFile.${it.key}" }
    mergeFunc.addStatement(statements)
    mergeFunc.addStatement("return this")
    return mergeFunc
}

fun generateReadme(jsonObject: Map<String, Option>) {
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
        val longName = if (it.value.fullName.isNotEmpty()) it.value.fullName else it.key
        val description = it.value.description
        var default = it.value.default
        // If some option have user defined type, then we will print to the README
        // only the value (e.g. LanguageType.UNDEFINED --> UNDEFINED)
        if (default != "null") {
            if (it.value.kotlinType != "kotlin.String") {
                default = default.substringAfterLast(".")
            }
        } else {
            default = "-"
        }
        readmeContent +=  "\n| $shortName | $longName | $description | $default |"
    }
    File("$generatedOptionsTablePath/OptionsTable.md").writeText(readmeContent)
}
