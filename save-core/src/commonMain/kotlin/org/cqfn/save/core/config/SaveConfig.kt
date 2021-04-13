package org.cqfn.save.core.config

import kotlinx.serialization.Serializable
import okio.ExperimentalFileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@OptIn(ExperimentalFileSystem::class)
@Serializable
data class SaveConfig(
    @Serializable(with = PathDeserializer::class)
    val propertiesFile: Path,
    val savePropertiesConfig: SavePropertiesConfig,
)

object PathDeserializer : KSerializer<Path> {
    override val descriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = decoder.decodeString().toPath()

    override fun serialize(encoder: Encoder, value: Path) =
        TODO("To serialize Path, FileSystem is needed to get the full canonical path")
}

/**
 * Configuration properties of save application, retrieved either from properties file
 * or from CLI args.
 * @property configPath path to the configuration file
 * @property parallelMode whether to enable parallel mode
 * @property threads number of threads
 * @property debug turn on debug logging
 * @property quiet do not log anything
 * @property reportType type of generated report with execution results
 * @property baselinePath path to the file with baseline data
 * @property excludeSuites test suites, which won't be checked
 * @property includeSuites test suites, only which ones will be checked
 * @property language language that you are developing analyzer for
 * @property testRootPath path to directory with tests
 * @property resultOutput data output stream
 * @property configInheritance whether configuration files should inherit configurations from the previous level of directories
 * @property ignoreSaveComments if true, ignore technical comments, that SAVE uses to describe warnings, when running tests
 * @property reportDir path to directory where to store output (when `resultOutput` is set to `FILE`)
 */
@Serializable
data class SavePropertiesConfig (
    @Serializable(with = PathDeserializer::class)
    val configPath: Path = "save.toml".toPath(),
    val parallelMode: Boolean = false,
    val threads: Int = 1,
    val debug: Boolean = false,
    val quiet: Boolean = false,
    val reportType: ReportType = ReportType.JSON,
    @Serializable(with = PathDeserializer::class)
    val baselinePath: Path? = null,
    val excludeSuites: String = "",
    val includeSuites: String = "",
    val language: LanguageType = LanguageType.UNDEFINED,
    @Serializable(with = PathDeserializer::class)
    val testRootPath: Path? = null,
    val configInheritance: Boolean = true,
    val ignoreSaveComments: Boolean = false,
    @Serializable(with = PathDeserializer::class)
    val reportDir: Path = "save-reports".toPath(),
    val resultOutput: ResultOutputType = ResultOutputType.STDOUT
)
