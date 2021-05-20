plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.cqfn.diktat:diktat-gradle-plugin:0.5.3")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.16.0")
    runtimeOnly(kotlin("gradle-plugin", "1.5.0"))
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("com.palantir.gradle.gitversion:gradle-git-version:0.12.3")
    implementation("com.squareup:kotlinpoet:1.8.0")
    implementation("com.google.code.gson:gson:2.8.6")
}
