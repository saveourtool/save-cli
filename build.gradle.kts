import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configurePublishing
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.disableRedundantKmpTargets
import org.cqfn.save.buildutils.installGitHooks

plugins {
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.cdsap.talaiot.plugin.base") version "1.4.2"
}

configureVersioning()

allprojects {
    repositories {
        mavenCentral()
    }
    configureDiktat()
    configureDetekt()

    disableRedundantKmpTargets()
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
