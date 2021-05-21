package org.cqfn.save.core.files

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.logging.logDebug
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlKeyValue
import com.akuleshov7.ktoml.parsers.node.TomlNode
import com.akuleshov7.ktoml.parsers.node.TomlTable
import okio.FileSystem

/**
 * A class that is capable for merging inherited configurations
 */
class MergeConfigs {
    /**
     * Merge parent configurations with current and prolong it for all child configs
     *
     * @param testConfig - testing SAVE config (save.toml) which should be merged
     */
    fun merge(testConfig: TestConfig) {
        logDebug("Start merge configs for ${testConfig.location}")
        val parentConfigs = collectParentConfigs(testConfig)
        mergeConfigs(parentConfigs)
        val childConfigs = collectChildConfigs(testConfig)
        mergeConfigs(childConfigs)
    }

    // Create the list of parent configs
    private fun collectParentConfigs(testConfig: TestConfig): MutableList<TestConfig> {
        val configList = mutableListOf(testConfig)
        var parentConfig = testConfig.parentConfig
        while (parentConfig != null) {
            configList.add(parentConfig)
            parentConfig = parentConfig.parentConfig
        }
        configList.reverse()
        return configList
    }

    // Create the list of child configs
    private fun collectChildConfigs(testConfig: TestConfig): MutableList<TestConfig> {
        // TODO:
        return mutableListOf()
    }

    // Merge configurations
    private fun mergeConfigs(configList: MutableList<TestConfig>) {
        if (configList.size == 1) {
            return
        }
        val pairs = configList.zipWithNext()

        pairs.forEach { (parent, child) ->
            logDebug("Merging ${parent.location} with ${child.location}")
            val parentConfig = TomlParser(parent.location.toString()).readAndParseFile()
            val childConfig = TomlParser(child.location.toString()).readAndParseFile()

            parentConfig.getAllChildTomlTables().forEach { parentTable ->
                val parentTableName = parentTable.fullTableName
                val childTable = childConfig.getAllChildTomlTables().find { it.fullTableName == parentTableName }
                childTable?.let {
                    val childTableContent = childTable.children
                    val childKeys: MutableList<String> = mutableListOf()
                    childTableContent.forEach { childKeys.add((it as TomlKeyValue).key.content) }
                    parentTable.children.forEach {
                        if ((it as TomlKeyValue).key.content !in childKeys) {
                            childTableContent.add(it)
                        }
                    }
                }
                    ?: run {
                        childConfig.appendChild(parentTable)
                    }
            }
            val newConfig = childConfig.asString()
            // logDebug("Merged config:\n----\n${newConfig}\n----")
            FileSystem.SYSTEM.write(child.location) {
                write(newConfig.encodeToByteArray())
            }
        }
    }
}

/**
 * Function extension. Returns a string representation of the object.
 *
 * @return corresponding representation
 */
fun TomlNode.asString() = tomlNodeToString(this).trimEnd()

/**
 * Returns a string representation of the object.
 *
 * @param node [node] to be represented by string
 * @return corresponding representation
 */
fun tomlNodeToString(node: TomlNode): String {
    var tomlData = ""
    if (node.name != "rootNode") {
        tomlData += (node.content) + "\n"
    }
    node.children.forEach { child ->
        tomlData += tomlNodeToString(child)
    }
    if (node is TomlTable) {
        tomlData += "\n"
    }
    return tomlData
}
