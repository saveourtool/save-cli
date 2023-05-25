package com.saveourtool.save.core.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AnnotationForMethods(
    val sourcePackage: String,
    val headerPackage: String = "",
    val jarPackage: String = "",
    val databaseName: String,
    val ruleName: String,
    val resultSize: Int = 1,
    val valid: Boolean = true
)