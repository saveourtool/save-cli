@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils


import platform.posix.system

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal {
    actual fun exec(cmd: String): Int {
        return system(cmd)
    }
}
