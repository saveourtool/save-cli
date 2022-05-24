
import com.saveourtool.save.buildutils.configureDiktat
import com.saveourtool.save.buildutils.configurePublishing
import com.saveourtool.save.buildutils.configureVersioning
import com.saveourtool.save.buildutils.createDetektTask
import com.saveourtool.save.buildutils.installGitHooks

plugins {
    id("com.cdsap.talaiot.plugin.base") version "1.4.2"
}

configureVersioning()

configureDiktat()
createDetektTask()
installGitHooks()

configurePublishing()

talaiot {
    publishers {
        timelinePublisher = true
    }
}
