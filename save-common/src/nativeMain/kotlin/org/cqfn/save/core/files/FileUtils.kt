/**
 * File Utils for native platforms
 */

package org.cqfn.save.core.files

import org.cqfn.save.core.logging.logDebug

import okio.FileSystem
import okio.Path
import platform.posix.*
import platform.posix.FTW_DEPTH
import platform.posix.open
import platform.posix.remove

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString

actual val fs: FileSystem = FileSystem.SYSTEM

actual fun FileSystem.createFile(path: Path, overwrite: Boolean): Path {
    if (overwrite) {
        remove(path.toString())
    }
    val fd = open(
        path.toString(),
        O_RDWR.or(O_CREAT),
        S_IRUSR.or(S_IWUSR).or(S_IRGRP).or(S_IROTH)
            .toUShort()
    )
    close(fd)
    return path
}

actual fun FileSystem.myDeleteRecursively(path: Path) {
    platform.posix.nftw(path.toString(), staticCFunction<CPointer<ByteVar>?, CPointer<stat>?, Int, CPointer<FTW>?, Int> { p, _, _, _ ->
        val fileName = p!!.toKString()
        logDebug("Attempt to delete file $fileName")
        remove(fileName)
    }, 64, FTW_DEPTH)
}
