plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.cqfn.diktat:diktat-gradle-plugin:1.0.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.19.0")
    implementation(kotlin("gradle-plugin", "1.6.0"))
    implementation(kotlin("serialization", "1.6.0"))
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.1")
    implementation("org.ajoberstar.grgit:grgit-core:4.1.1")
    implementation("com.squareup:kotlinpoet:1.10.2")
    implementation("com.google.code.gson:gson:2.8.9")
}
