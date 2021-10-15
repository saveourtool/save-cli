package org.cqfn.save.core.utils

class ProcessTimeoutException(val timeoutMillis: Long, message: String) : ProcessExecutionException(message)
