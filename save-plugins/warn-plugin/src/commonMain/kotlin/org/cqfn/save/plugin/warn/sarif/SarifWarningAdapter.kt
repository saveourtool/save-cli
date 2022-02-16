/**
 * Methods for conversion from SARIF model to [Warning]s
 */

package org.cqfn.save.plugin.warn.sarif

import org.cqfn.save.plugin.warn.utils.Warning

import io.github.detekt.sarif4k.Location
import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import okio.Path
import okio.Path.Companion.toPath

/**
 * Convert this SARIF report to a list of [Warning]s.
 *
 * @param testFiles if this list is not empty, then results from SARIF will be filtered to match paths from [testFiles].
 * [testFiles] should be relative to test root, then URIs from SARIF will be trimmed too and matched against [testFiles].
 * Regarding relative paths in SARIF see [this comment](https://github.com/microsoft/sarif-vscode-extension/issues/294#issuecomment-657753955).
 * @param testRoot a root directory of test suite
 * @param workingDirectory initial working directory, when SAVE started
 * @return a list of [Warning]s
 */
fun SarifSchema210.toWarnings(
    testRoot: Path?,
    testFiles: List<Path>,
    workingDirectory: Path
): List<Warning> {
    // "Each run represents a single invocation of a single analysis tool, and the run has to describe the tool that produced it."
    // In case of SAVE this array will probably always have a single element.
    return runs.flatMap {
        it.toWarning(testRoot, testFiles, workingDirectory)
    }
}

/**
 * @param testRoot a root directory of test suite
 * @param testFiles if this list is not empty, then results from SARIF will be filtered to match paths from [testFiles].
 * @param workingDirectory initial working directory, when SAVE started
 * @return a list of [Warning]s
 */
@Suppress("TOO_LONG_FUNCTION", "AVOID_NULL_CHECKS")
fun Run.toWarning(
    testRoot: Path?,
    testFiles: List<Path>,
    workingDirectory: Path
): List<Warning> {
    // "A result is an observation about the code."
    return results?.map { result ->
        // "array of location objects which almost always contains exactly one element"
        // Location is empty for warnings that, e.g., refer to the whole project instead of individual files.
        // Location can have >1 elements, e.g., if the warning suggests a refactoring, that affects multiple files.
        val filePath = result.locations
            ?.singleOrNull()
            ?.extractFilePath(testRoot, workingDirectory)
        result to filePath
    }
        ?.filter { (_, filePath) ->
            testFiles.isEmpty() || filePath in testFiles
        }
        ?.map { (result, filePath) ->
            val (line, column) = result.locations?.map {
                // "The most common case is for a tool to report a physical location, and to specify the location by line and column number."
                it.extractLineColumn()
            }
                ?.singleOrNull()
                ?: (null to null)
            val fileName = filePath?.name ?: ""
            Warning(
                // in the simplest case, Message will only contain `text`
                message = result.message.text ?: "",
                line = line?.toInt(),
                column = column?.toInt(),
                fileName = fileName,
            )
        }
        ?: emptyList()
}

private fun Location.extractLineColumn(): Pair<Long?, Long?>? = physicalLocation?.region
    ?.let {
        it.startLine to it.startColumn
    }

@Suppress("TOO_MANY_LINES_IN_LAMBDA")
private fun Location.extractFilePath(testRoot: Path?, workingDirectory: Path) = physicalLocation
    ?.artifactLocation
    ?.uri
    // assuming that all URIs for SAVE correspond to files
    ?.dropFileProtocol()
    ?.toPath()
    ?.let {
        require(!(it.isAbsolute && testRoot == null)) {
            "If paths in SARIF report are absolute, testRoot is required to resolve them: " +
                    "couldn't convert path $it to relative"
        }
        if (it.isAbsolute) {
            val absoluteTestRootPath = if (!testRoot!!.isAbsolute) {
                // relativeTo method requires paths, which contains some root for proper comparison,
                // i.e. simple name couldn't be compared with path: `/some/nested/path` and `path`
                // so we use following trick, to calculate absolute path of testRoot:
                // get initial working directory + resolve it regarding relative path to the testRoot = absolute path of the testRoot
                workingDirectory.resolve(testRoot).normalized()
            } else {
                testRoot
            }
            it.relativeTo(absoluteTestRootPath)
        } else {
            it
        }
    }
