package org.cqfn.save.plugin.warn.sarif

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.files.parents

fun FileSystem.findSarifUpper(path: Path, sarifFileName: String): Path? {
    return path.parents().firstOrNull { parent ->
        metadata(parent).isDirectory && list(parent).any {
            it.name == sarifFileName
        }
    }
}

fun List<Path>.adjustToCommonRoot(root: Path) = map {
    it.relativeTo(root).normalized()
}

internal fun FileSystem.topmostTestDirectory(path: Path): Path {
    return path.parents().last { parent ->
        list(parent).any { it.name == "save.toml" }
    }
}
