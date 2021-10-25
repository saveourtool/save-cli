// ;warn:1:1: [HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE] files that contain multiple or no classes should contain description of what is inside of this file: there are 2 declared classes and/or objects (cannot be auto-corrected)
package org.cqfn.save.chapter2

import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.Properties

/**
 * @property foo
 * @property bar
 */
@ExperimentalStdlibApi public data class Example(val foo: Int, val bar: Double) : SuperExample("lorem ipsum")

private class TestException : Exception()
/* this class is unused */
// private class Test : RuntimeException()

/**
 * @param runConfiguration
 * @param containerName
 * @param file
 * @param resources
 */
internal fun createWithFile(
    runConfiguration: RunConfiguration,
    containerName: String,
    file: File,
    resources: Collection<File> = emptySet()
    // ;warn:30:3: [EMPTY_BLOCK_STRUCTURE_ERROR] incorrect format of empty block: empty blocks are forbidden unless it is function with override keyword (cannot be auto-corrected)
) {}

private fun foo(node: ASTNode) {
    when (node.elementType) {
        CLASS, FUN, PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR -> checkAnnotation(node)
        else -> {
            // this is a generated else block
        }
    }
    val qwe = a &&
            // ;warn:41:1: [WRONG_INDENTATION] only spaces are allowed for indentation and each indentation should equal to 4 spaces (tabs are not allowed): expected 12 but was 8
        b
    val qwe = a &&
            // ;warn:44:1: [WRONG_INDENTATION] only spaces are allowed for indentation and each indentation should equal to 4 spaces (tabs are not allowed): expected 12 but was 8
        b

    // comment
    if (x) {
        foo()
    }

    setOf<Object>(IOException(), Properties(), LoggerFactory())
}
