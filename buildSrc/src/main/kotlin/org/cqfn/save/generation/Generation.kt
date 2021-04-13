/**
 * This file contains code for codegen: generating a list of options for config files and README.
 */

package org.cqfn.save.generation

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

import java.io.File
import java.io.BufferedReader

/**
 * The comment that will be added to the generated sources file.
 */
private val autoGenerationComment =
    """
    | This file was auto generated, please don't modify it.
    """.trimMargin()

// Paths, where to store generated files
val generatedSaveConfigPath = "save-core/src/commonMain/kotlin/org/cqfn/save/core/config/"
val generatedConfigPath = "save-cli/src/nativeMain/kotlin/org/cqfn/save/cli/"
val generatedOptionsTablePath = "buildSrc/src/main/kotlin/org/cqfn/save/generation/"

/**
 * This class represents the general form of each key in json file with config options
 * @property argType Type which will be used in ArgParser in Config.kt
 * @property kotlinType Type which will be used in kotlin code
 * @property fullName Full name of option for usage in Save cli
 * @property shortName Short name of option for usage in Save cli
 * @property description Option description
 * @property option Additional argument, which will be used in Config.kt (default(), multiple(), required(), etc)
 */
class Option {
    lateinit var argType: String
    lateinit var kotlinType: String
    lateinit var fullName: String
    lateinit var shortName: String
    lateinit var description: String
    lateinit var option: Map<String, String>
}

fun main() {
    println("======================")
    val configFile = "buildSrc/src/main/kotlin/config-options.json"
    val gson = Gson()
    val bufferedReader: BufferedReader = File(configFile).bufferedReader()
    val jsonString = bufferedReader.use { it.readText() }
    val jsonObject = gson.fromJson<Map<String, Option>>(jsonString, object : TypeToken<Map<String, Option>>(){}.type)
    generateConfig(jsonObject)
    generateSaveConfig(jsonObject)
    generateReadme(jsonObject)
    println("\n======================")
}

fun generateConfig(jsonObject: Map<String, Option>) {
    println("--------------generateConfig-----------------------")
    val builder = FileSpec.builder("org.cqfn.save.cli", "Config")
    builder.addComment(autoGenerationComment)
    builder.addImport("org.cqfn.save.core.config", "LanguageType")
    builder.addImport("org.cqfn.save.core.config", "ReportType")
    builder.addImport("org.cqfn.save.core.config", "ResultOutputType")
    builder.addImport("kotlinx.cli", "ArgParser")
    builder.addImport("kotlinx.cli", "ArgType")

    val kdoc =
             """
             | @param args CLI args
             | @return an instance of [SaveConfig]
             | @throws e
             """.trimMargin()

    val funcBuilder = FunSpec.builder("createConfigFromArgs")
        .addKdoc(kdoc)
        .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("\"TOO_LONG_FUNCTION\"").build())
        .addParameter("args", ClassName("kotlin", "Array")
                                        .parameterizedBy(ClassName("kotlin", "String")))
        .returns(ClassName("org.cqfn.save.core.config","SaveConfig"))
        .addStatement("    val parser = ArgParser(\"save\")")
        .addStatement(addOptions(jsonObject))
        .build()

    builder.addFunction(funcBuilder)
    builder.build().writeTo(System.out)
    //File("$generatedConfigPath/Config.kt").writeText(builder.build().toString())
    println("-------------------------------------")
}

// Add options for ArgParser in Config.kt
fun addOptions(jsonObject: Map<String, Option>): String {
    var options = "    "
    jsonObject.forEach {
        options += "val ${it.key} by parser.option(\n" +
                        "    ${it.value.argType},\n"
        val fullName = if (it.value.fullName.isNotEmpty()) "    fullName = \"${it.value.fullName}\",\n" else ""
        if (fullName.isNotEmpty()) {
            options += fullName
        }
        val shortName = if (it.value.shortName.isNotEmpty()) "    shortName = \"${it.value.shortName}\",\n" else ""
        if (shortName.isNotEmpty()) {
            options += shortName
        }
        // We replace whitespaces to `·`, in aim to avoid incorrect line breaking,
        // which could be done by kotlinpoet
        options += "    description = \"${it.value.description.replace(" ", "·")}\"\n" +
                            ")\n"
    }
    return options
}

fun generateSaveConfig(jsonObject: Map<String, Option>) {
    println("-----------generateSaveConfig------------")
    val builder = FileSpec.builder("org.cqfn.save.core.config", "SaveConfig")
    builder.addComment(autoGenerationComment)
    builder.addImport("okio", "ExperimentalFileSystem")

    var properties = ""
    jsonObject.forEach { properties += ("@property ${it.key} ${it.value.description}\n")}
    val kdoc = """
               |Configuration properties of save application, retrieved either from properties file
               |or from CLI args.
               |$properties
               """.trimMargin()

    val classBuilder = TypeSpec.classBuilder("SaveConfig").addModifiers(KModifier.DATA)
    classBuilder.addKdoc(kdoc)

    val experimentalFileSystem = AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                                                            .addMember("ExperimentalFileSystem::class")
    classBuilder.addAnnotation(experimentalFileSystem.build())

    val ctor = FunSpec.constructorBuilder()

    for ((name, value) in jsonObject) {
        ctor.addParameter(name, selectType(value))
        val property = PropertySpec.builder(name, selectType(value)).initializer(name).build()
        classBuilder.addProperty(property)
    }
    classBuilder.primaryConstructor(ctor.build())

    builder.addType(classBuilder.build())
    builder.build().writeTo(System.out)
    //File("$generatedSaveConfigPath/SaveConfig.kt").writeText(builder.build().toString())
    println("-------------------------------------")
}

fun selectType(value: Option): TypeName =
    when(value.kotlinType) {
        "Boolean" -> ClassName("kotlin", "Boolean")
        "Int" -> ClassName("kotlin", "Int")
        "String" -> ClassName("kotlin", "String")
        "List<String>" -> ClassName("kotlin.collections", "List")
                              .parameterizedBy(ClassName("kotlin", "String"))
        "Path" -> ClassName("okio", "Path")
        "Path?" -> ClassName("okio", "Path").copy(nullable = true)
        "ReportType" -> ClassName("org.cqfn.save.core.config","ReportType")
        "LanguageType" -> ClassName("org.cqfn.save.core.config","LanguageType")
        "ResultOutputType" -> ClassName("org.cqfn.save.core.config","ResultOutputType")
        else -> ClassName("kotlin", "Unit")
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
        val default = if ("default" in it.value.option.keys) it.value.option["default"] else "-"
        readmeContent +=  "\n| $shortName | $longName | $description | $default |"
    }
    println(readmeContent)
    File("$generatedOptionsTablePath/OptionsTable.md").writeText(readmeContent)
}
