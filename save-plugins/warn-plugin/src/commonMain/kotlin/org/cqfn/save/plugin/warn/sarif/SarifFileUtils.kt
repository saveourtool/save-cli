package org.cqfn.save.plugin.warn.sarif

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.files.parents

fun FileSystem.findSarifUpper(path: Path): Path? {
    return path.parents().firstOrNull { parent ->
        metadata(parent).isDirectory && list(parent).any {
            it.name == "save-warnings.sarif"
        }
    }
}
