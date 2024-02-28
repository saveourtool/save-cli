package com.saveourtool.save.core.annotations

enum class CheckedLanguage {
    foo,
    bar,
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AnnotationForClass(
    val databaseSuffix: String,
    val ruleSuffix: String = "txt",
    val codeBasePath: String,
    val databasePath: String,
    val ruleBasePath: String,
    val checkedLanguage: CheckedLanguage,
    val needParseSourceFile: Boolean = true
)

