plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.cqfn.diktat:diktat-gradle-plugin:0.4.2")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.15.0")
    implementation("com.squareup:kotlinpoet:1.8.0")
    implementation ("com.google.code.gson:gson:2.8.6")
}