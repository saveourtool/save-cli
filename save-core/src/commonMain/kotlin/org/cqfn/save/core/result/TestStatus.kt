package org.cqfn.save.core.result

sealed class TestStatus

object Success : TestStatus()

data class Failure(val reason: String) : TestStatus()

data class Ignored(val reason: String) : TestStatus()

data class Crash(val throwable: Throwable) : TestStatus()