import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configurePublishing
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.installGitHooks

plugins {
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.github.ben-manes.versions") version "0.38.0"
    id("com.cdsap.talaiot.plugin.base") version "1.4.2"
}

configureVersioning()

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") {
            // detekt requires kotlinx.html (until detekt version 1.17)
            content {
                includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
            }
        }
    }
    configureDiktat()
    configureDetekt()
}
createDiktatTask()
createDetektTask()
installGitHooks()

configurePublishing()

talaiot {
    publishers {
        timelinePublisher = true
    }
}
