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
    implementation("org.cqfn.diktat:diktat-gradle-plugin:1.0.2")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.19.0")
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.plugin.serialization)
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.1")
    implementation("org.ajoberstar.grgit:grgit-core:4.1.1")
    implementation("com.squareup:kotlinpoet:1.10.2")
    implementation("com.google.code.gson:gson:2.8.9")
}
