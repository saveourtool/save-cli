package org.cqfn.save.plugins.fixandwarn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.plugin.Plugin

class FixAndWarnPlugin(
    testConfig: TestConfig,
    testFiles: List<String>,
    useInternalRedirections: Boolean = true) : Plugin(
    testConfig,
    testFiles,
    useInternalRedirections) {

    }