/**
 * File Utils for JVM
 */

@file:JvmName("FileUtilsJvm")

package org.cqfn.save.core.files

import okio.FileSystem

actual val fs: FileSystem = FileSystem.SYSTEM
