package com.saveourtool.save.core.utils

import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PropertiesFileUtilsTest {
    private val fs: FileSystem = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / PropertiesFileUtilsTest::class.simpleName!!)


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
            PropertiesFileUtils.read(fs, testFile)
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
            PropertiesFileUtils.read(fs, testFile)
        }
    }

    @Test
    fun `read not existed properties file`() {
        val testFile = tmpDir / "not-existed.properties"
        require(!fs.exists(testFile))


        assertEquals(
            emptyMap(),
            PropertiesFileUtils.read(fs, testFile)
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
}
