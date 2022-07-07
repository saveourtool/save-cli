/**
 * This file is a workaround to several issues that we don't have time to investigate:
 * 1) shitty IDEA bugs that prevent resolving of FileSystem.SYSTEM
 * 2) gradle kotlin plugin bugs that block the resolving of FileSystem.SYSTEM in native projects
 */

package com.saveourtool.save.cli

import okio.FileSystem

actual val fs: FileSystem = FileSystem.SYSTEM
