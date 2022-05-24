package com.saveourtool.save.core.utils

/**
 * @property timeoutMillis
 */
class ProcessTimeoutException(val timeoutMillis: Long, message: String) : ProcessExecutionException(message)
