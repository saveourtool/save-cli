package org.cqfn.save.plugin.warn

import org.cqfn.save.core.plugin.PluginException

/**
 * An [Exception] that can be thrown when parsing a file
 */
class ResourceFormatException(message: String) : PluginException(message)
