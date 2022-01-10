
import org.cqfn.save.buildutils.configurePublishing
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.installGitHooks

plugins {
    id("com.cdsap.talaiot.plugin.base") version "1.4.2"
}

configureVersioning()

createDiktatTask()
createDetektTask()
installGitHooks()

configurePublishing()

talaiot {
    publishers {
        timelinePublisher = true
    }
}
