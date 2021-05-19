plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.cqfn.diktat:diktat-gradle-plugin:0.5.3")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.17.0")
    runtimeOnly(kotlin("gradle-plugin", "1.5.0"))
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.0")
    implementation("com.squareup:kotlinpoet:1.8.0")
    implementation("com.google.code.gson:gson:2.8.6")
}
