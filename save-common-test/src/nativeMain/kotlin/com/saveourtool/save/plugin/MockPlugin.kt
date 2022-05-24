/**
 * Platform-specific declarations
 */

package com.saveourtool.save.plugin

import okio.FileSystem

internal actual val fs: FileSystem = FileSystem.SYSTEM
