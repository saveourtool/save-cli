import org.cqfn.save.generation.main
import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.installGitHooks

plugins {
    kotlin("multiplatform") version Versions.kotlin apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.github.ben-manes.versions") version "0.38.0"
    id("com.cdsap.talaiot.plugin.base") version "1.4.2"
}

version = "0.1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }
    configureDiktat()
    configureDetekt()
}
createDiktatTask()
createDetektTask()
installGitHooks()

talaiot {
    publishers {
        timelinePublisher = true
    }
}

tasks.register("generateConfigOptions") { main() }