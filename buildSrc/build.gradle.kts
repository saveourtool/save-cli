plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // workaround https://github.com/gradle/gradle/issues/15383
    implementation(files(project.libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.diktat.gradle.plugin)
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.22.0")
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.plugin.serialization)
    implementation("io.github.gradle-nexus:publish-plugin:1.3.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.16.1")
    implementation("com.squareup:kotlinpoet:1.13.0")
    implementation("com.google.code.gson:gson:2.10.1")
}
