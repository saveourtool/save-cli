package com.saveourtool.save.core.utils

import okio.FileSystem
import okio.IOException

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

class CliUtilsTest {
    private val fs: FileSystem = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FileSystem::class.simpleName!!)

    @Test
    fun `parse properties file`() {
        val testFile = tmpDir / "test.properties"
        fs.write(testFile) {
            this.write("key1=value1\n".encodeToByteArray())
            this.write("key2=value2\n".encodeToByteArray())
        }

        assertEquals(
            TestProperties("value1", "value2"),
            fs.parsePropertiesFile(tmpDir.toString(), "test")
        )
    }

    @Test
    fun `parse properties file from not existed folder`() {
        assertFailsWith<IOException> {
            fs.parsePropertiesFile<TestProperties>(tmpDir.resolve("not-existed").toString(), "test")
        }
    }

    @Test
    fun `resolve value`() {
        val invalidParser = ArgParser("invalid")
        val parser = ArgParser("test")
        val key1Option = parser.option(ArgType.String, "key1").default("default_value1")
        val key1 by key1Option
        val key2Option = parser.option(ArgType.String, "key2").default("default_value2")
        val key2 by key2Option
        val key3Option = invalidParser.option(ArgType.String, "key3").default("default_value3")
        val key3 by key3Option

        parser.parse(listOf("--key1", "some_value1").toTypedArray())

        assertEquals("some_value1", key1)
        assertEquals(
            "cli_value1",
            key1Option.resolveValue("cli_value1", "override_value1")
        )

        assertEquals("default_value2", key2)
        assertEquals(
            "override_value2",
            key2Option.resolveValue("cli_value2", "override_value2")
        )

        assertFailsWith<RuntimeException> {
            // touch variable
            key3.length
        }
        assertEquals(
            "override_value3",
            key3Option.resolveValue("cli_value3", "override_value3")
        )
    }

    @Test
    fun `read properties file`() {
        val testFile = tmpDir / "default.properties"
        fs.write(testFile) {
            this.write("key1=value1\n".encodeToByteArray())
            this.write("key2=value2\n".encodeToByteArray())
            this.write("key3=value31=value32\n".encodeToByteArray())
        }

        assertEquals(
            mapOf("key1" to "value1", "key2" to "value2", "key3" to "value31=value32"),
            fs.readPropertiesFile(testFile)
        )
    }

    @Test
    fun `read invalid properties file`() {
        val testFile = tmpDir / "invalid.properties"
        fs.write(testFile) {
            this.write("key1=value1\n".encodeToByteArray())
            this.write("key2\n".encodeToByteArray())
        }

        assertFailsWith<IllegalArgumentException> {
            fs.readPropertiesFile(testFile)
        }
    }

    @Test
    fun `read not existed properties file`() {
        val testFile = tmpDir / "not-existed.properties"
        require(!fs.exists(testFile))

        assertEquals(
            emptyMap(),
            fs.readPropertiesFile(testFile)
        )
    }

    @BeforeTest
    fun createTmpFolder() {
        fs.createDirectory(tmpDir)
    }

    @AfterTest
    fun cleanupTmpFolder() {
        fs.deleteRecursively(tmpDir)
    }

    /**
     * @property key1
     * @property key2
     */
    @kotlinx.serialization.Serializable
    private data class TestProperties(
        val key1: String,
        val key2: String
    )
}
