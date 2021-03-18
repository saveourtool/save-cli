package org.cqfn.save.core.plugin

interface Plugin {
    fun execute(configFileLines: List<String>)
}
