package org.cqfn.save.core.files

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.logging.logDebug
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlFile
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
        mergeConfigList(parentConfigs)
        val childConfigs = collectChildConfigs(testConfig)
        mergeConfigList(childConfigs)
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
        return mutableListOf(testConfig)
    }

    // Merge configurations
    private fun mergeConfigList(configList: MutableList<TestConfig>) {
        if (configList.size == 1) {
            return
        }
        val pairs = configList.zipWithNext()

        pairs.forEach { (parent, child) ->
            logDebug("Merging ${parent.location} with ${child.location}")
            val parentConfig = TomlParser(parent.location.toString()).readAndParseFile()
            val childConfig = TomlParser(child.location.toString()).readAndParseFile()
            mergeChildConfigWithParent(parentConfig, childConfig)
            val newConfig = childConfig.asString()
            // logDebug("Merged config:\n----\n${newConfig}\n----")
            FileSystem.SYSTEM.write(child.location) {
                write(newConfig.encodeToByteArray())
            }
        }
    }

    private fun mergeChildConfigWithParent(parentConfig: TomlFile, childConfig: TomlFile) {
        parentConfig.getAllChildTomlTables().forEach { parentTable ->
            val parentTableName = parentTable.fullTableName
            // If there will be found table in child config with the same name, than content should be merged
            val childTable = childConfig.getAllChildTomlTables().find { it.fullTableName == parentTableName }
            childTable?.let {
                mergeFieldsFromTomlTable(parentTable, childTable)
            }
            // There is now such table in child config, just add it whole
                ?: run {
                    childConfig.appendChild(parentTable)
                }
        }
    }

    private fun mergeFieldsFromTomlTable(parentTable: TomlTable, childTable: TomlTable) {
        // Looking for fields from parent config, which are absent in child config, they should be added
        // We will compare fields by keys from TomlKeyValue
        val childTableFields = childTable.children
        val childKeys: MutableList<String> = mutableListOf()
        childTableFields.forEach { childKeys.add((it as TomlKeyValue).key.content) }
        parentTable.children.forEach {
            if ((it as TomlKeyValue).key.content !in childKeys) {
                childTableFields.add(it)
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
