import io.gitlab.arturbosch.detekt.Detekt
import org.cqfn.save.buildutils.configurePublishing
import org.cqfn.save.generation.configFilePath
import org.cqfn.save.generation.generateConfigOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    val hostTarget = listOf(linuxX64(), mingwX64(), macosX64())

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val commonMain by getting {
            dependencies {
                implementation(project(":save-common"))
                api("com.squareup.okio:okio-multiplatform:${Versions.okio}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:${Versions.Kotlinx.cli}")
                implementation("com.akuleshov7:ktoml-core:${Versions.ktoml}")
                implementation(project(":save-plugins:fix-plugin"))
                implementation(project(":save-plugins:warn-plugin"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        hostTarget.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }

        val nativeTest by creating {
            dependsOn(commonTest)
        }
        hostTarget.forEach {
            getByName("${it.name}Test").dependsOn(nativeTest)
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
            }
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

val generateCodeTaskProvider = tasks.register("generateConfigOptions") {
    inputs.file(configFilePath())
    val generatedFile = File("$buildDir/generated/src/org/cqfn/save/core/config/SaveProperties.kt")
    outputs.file(generatedFile)
    doFirst {
        generateConfigOptions(generatedFile)
    }
}
val generatedKotlinSrc = kotlin.sourceSets.create("generated") {
    kotlin.srcDir("$buildDir/generated/src")
}
kotlin.sourceSets.getByName("commonMain").dependsOn(generatedKotlinSrc)
tasks.withType<KotlinCompile<*>>().forEach {
    it.dependsOn(generateCodeTaskProvider)
}
