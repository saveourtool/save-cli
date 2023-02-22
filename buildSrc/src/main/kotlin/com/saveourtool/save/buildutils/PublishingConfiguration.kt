/**
 * Publishing configuration file.
 */

package com.saveourtool.save.buildutils

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

@Suppress(
    "MISSING_KDOC_ON_FUNCTION",
    "MISSING_KDOC_TOP_LEVEL",
    "TOO_LONG_FUNCTION"
)
fun Project.configurePublishing() {
    // If present, set properties from env variables. If any are absent, release will fail.
    System.getenv("OSSRH_USERNAME")?.let {
        extra.set("sonatypeUsername", it)
    }
    System.getenv("OSSRH_PASSWORD")?.let {
        extra.set("sonatypePassword", it)
    }
    System.getenv("GPG_SEC")?.let {
        extra.set("signingKey", it)
    }
    System.getenv("GPG_PASSWORD")?.let {
        extra.set("signingPassword", it)
    }

    if (this == rootProject) {
        apply<NexusPublishPlugin>()
        if (hasProperty("sonatypeUsername")) {
            configureNexusPublishing()
        }
    }

    apply<MavenPublishPlugin>()
    apply<SigningPlugin>()

    configurePublications()

    if (hasProperty("signingKey")) {
        configureSigning()
    }

    // https://kotlinlang.org/docs/mpp-publish-lib.html#avoid-duplicate-publications
    // Publication with name `save` is for the default artifact.
    // `configureNexusPublishing` adds sonatype publication tasks inside `afterEvaluate`.
    afterEvaluate {
        val publicationsFromMainHost = listOf("jvm", "js", "linuxX64", "kotlinMultiplatform", "metadata")
        configure<PublishingExtension> {
            publications.matching { it.name in publicationsFromMainHost }.all {
                val targetPublication = this@all
                tasks.withType<AbstractPublishToMaven>()
                    .matching { it.publication == targetPublication }
                    .configureEach {
                        onlyIf {
                            // main publishing CI job is executed on Linux host
                            DefaultNativePlatform.getCurrentOperatingSystem().isLinux.apply {
                                if (!this) {
                                    logger.lifecycle("Publication ${(it as AbstractPublishToMaven).publication.name} is skipped on current host")
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "GENERIC_VARIABLE_WRONG_DECLARATION")
private fun Project.configurePublications() {
    val dokkaJarProvider = tasks.register<Jar>("dokkaJar") {
        group = "documentation"
        archiveClassifier.set("javadoc")
        from(tasks.findByName("dokkaHtml"))
    }
    configure<PublishingExtension> {
        repositories {
            mavenLocal()
        }
        publications.withType<MavenPublication>().forEach { publication ->
            publication.artifact(dokkaJarProvider)
            publication.pom {
                name.set(project.name)
                description.set(project.description ?: project.name)
                url.set("https://github.com/saveourtool/save")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("petertrr")
                        name.set("Petr Trifanov")
                        email.set("peter.trifanov@gmail.com")
                    }
                    developer {
                        id.set("akuleshov7")
                        name.set("Andrey Kuleshov")
                        email.set("andrewkuleshov7@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/saveourtool/save")
                    connection.set("scm:git:git://github.com/saveourtool/save.git")
                }
            }
        }
    }
}

private fun Project.configureSigning() {
    configure<SigningExtension> {
        useInMemoryPgpKeys(property("signingKey") as String?, property("signingPassword") as String?)
        logger.lifecycle("The following publications are getting signed: ${extensions.getByType<PublishingExtension>().publications.map { it.name }}")
        sign(*extensions.getByType<PublishingExtension>().publications.toTypedArray())
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        // Workaround for the problem described at https://github.com/saveourtool/save-cli/pull/501#issuecomment-1439705340.
        // We have a single Javadoc artifact shared by all platforms, hence all publications depend on signing of this artifact.
        // This causes weird implicit dependencies, like `publishJsPublication...` depends on `signJvmPublication`.
        dependsOn(tasks.withType<Sign>())
    }
}

private fun Project.configureNexusPublishing() {
    configure<NexusPublishExtension> {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                username.set(property("sonatypeUsername") as String)
                password.set(property("sonatypePassword") as String)
            }
        }
    }
}
