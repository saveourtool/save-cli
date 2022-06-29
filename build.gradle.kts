
import com.saveourtool.save.buildutils.configureDiktat
import com.saveourtool.save.buildutils.configurePublishing
import com.saveourtool.save.buildutils.configureVersioning
import com.saveourtool.save.buildutils.createDetektTask
import com.saveourtool.save.buildutils.installGitHooks

configureVersioning()

configureDiktat()
createDetektTask()
installGitHooks()

configurePublishing()
