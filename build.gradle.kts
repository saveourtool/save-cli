import com.saveourtool.save.buildutils.configureDiktat
import com.saveourtool.save.buildutils.configurePublishing
import com.saveourtool.save.buildutils.configureVersioning
import com.saveourtool.save.buildutils.createDetektTask
import com.saveourtool.save.buildutils.installGitHooks

// version generation
configureVersioning()
// checks and validations
configureDiktat()
createDetektTask()
installGitHooks()
// publishing to maven central
configurePublishing()
