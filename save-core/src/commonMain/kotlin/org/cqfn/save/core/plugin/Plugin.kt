package org.cqfn.save.core.plugin

/**
 * Plugin that can be injected into SAVE during execution. Plugins accept contents of configuration file and then perform some work.
 */
interface Plugin {
    /**
     * @param configFileLines contents of configuration file
     */
    fun execute(configFileLines: List<String>)
}
