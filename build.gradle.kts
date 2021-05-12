import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configurePublishing
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.installGitHooks
import org.cqfn.save.generation.generateConfigOptions

plugins {
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.github.ben-manes.versions") version "0.38.0"
    id("com.cdsap.talaiot.plugin.base") version "1.4.2"
}

configureVersioning()

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

configurePublishing()

talaiot {
    publishers {
        timelinePublisher = true
    }
}

tasks.register("generateConfigOptions") { generateConfigOptions() }
