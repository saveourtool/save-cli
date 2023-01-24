/**
 * File Utils for native platforms
 */

package com.saveourtool.save.core.files

import com.saveourtool.save.core.logging.logTrace

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.FTW
import platform.posix.FTW_DEPTH
import platform.posix.nftw
import platform.posix.remove
import platform.posix.stat

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString

actual val fs: FileSystem = FileSystem.SYSTEM

/**
 * Delete this directory and all other files and directories in it
 *
 * @param path a path to a directory
 */
@Suppress("MAGIC_NUMBER", "MagicNumber")
actual fun FileSystem.myDeleteRecursively(path: Path) {
    nftw(path.toString(), staticCFunction<CPointer<ByteVar>?, CPointer<stat>?, Int, CPointer<FTW>?, Int> { pathName, _, _, _ ->
        val fileName = pathName!!.toKString()
        logTrace("Attempt to delete file $fileName")
        remove(fileName)
    }, 64, FTW_DEPTH)
}

actual fun getWorkingDirectory(): Path = fs.canonicalize(".".toPath())
