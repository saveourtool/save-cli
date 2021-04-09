/**
 * This file contains code for codegen: generating a list of options for config files and README.
 */

package org.cqfn.save.generation

import com.squareup.kotlinpoet.FileSpec
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.File
import java.io.BufferedReader

/**
 * The comment that will be added to the generated sources file.
 */
private val autoGenerationComment =
    """
    | This document was auto generated, please don't modify it.
    """.trimMargin()

class Option {
    var type: String? = null
    var fullName: String? = null
    var shortName: String? = null
    var description: String?= null
    var option: Map<String, String>? = null
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
    jsonObject.forEach {
        val currOption = it.value
        println("${it.key} ${currOption.type}")
    }

    /* TODO
    val kotlinFile = FileSpec
        .builder("org.cqfn.save.cli", "Config")
        .addType(fileBody)
        .indent("    ")
        .addComment(autoGenerationComment)
        .build()

     */
}

fun generateSaveConfig(jsonObject: Map<String, Option>) {

}

fun generateReadme(jsonObject: Map<String, Option>) {

}
