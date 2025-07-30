package com.nerosec.xns.io.extensions

import com.nerosec.xns.io.CapacityType
import com.nerosec.xns.io.ListOption
import com.nerosec.xns.io.Venv
import com.nerosec.xns.io.extensions.PathExtensions.getContentType
import com.nerosec.xns.io.extensions.PathExtensions.getDirectoryEntries
import com.nerosec.xns.io.extensions.PathExtensions.getSize
import com.nerosec.xns.io.extensions.PathExtensions.getSizeInBytes
import com.nerosec.xns.io.extensions.PathExtensions.remove
import com.nerosec.xns.io.extensions.PathExtensions.removeDirectoryEntries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.math.BigDecimal
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

class PathExtensionsSpec {

    private val venv = Venv()

    @BeforeEach
    fun setup() {
        venv.setup()
    }

    @AfterEach
    fun cleanup() {
        venv.cleanup()
    }

    @Test
    fun `Produces IOException if attempting to retrieve directory entries for directory, that does not exist`() {
        assertThrows<IOException> { Path.of("TEST_DIRECTORY").getDirectoryEntries() }
    }

    @Test
    fun `Produces IOException if attempting to retrieve directory entries for a resource, that is not a directory`() {
        val dir = venv.create("TEST_FILE.txt")
        assertThrows<IOException> { dir.getDirectoryEntries() }
    }

    @Test
    fun `Finds no entries if attempting to retrieve directory entries at depth, that is less than 1`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        venv.create("TEST_DIRECTORY", "TEST_FILE.txt")
        assertEquals(0, dir.getDirectoryEntries(depth = 0).size)
    }

    @Test
    fun `Lists directory entries, based on specified depth`() {
        val dir = venv.createDirectory("TEST_DIRECTORY_A")
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_A.txt")
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_B.txt")
        venv.createDirectory("TEST_DIRECTORY_A", "TEST_DIRECTORY_B")
        venv.create("TEST_DIRECTORY_A", "TEST_DIRECTORY_B", "TEST_FILE_C.txt")
        assertEquals(3, dir.getDirectoryEntries(depth = 1).size)
    }

    @Test
    fun `Lists directory entries, but ignores hidden resources if ListOption#INCLUDE_HIDDEL_FILES is not specified`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        venv.create("TEST_DIRECTORY", "TEST_FILE.txt")
        venv.create("TEST_DIRECTORY", ".HIDDEN_TEST_FILE")
        assertEquals(1, dir.getDirectoryEntries(depth = 1, options = emptyArray<ListOption>()).size)
    }

    @Test
    fun `Produces IOException if attempting to remove directory entries for directory, that does not exist`() {
        assertThrows<IOException> { Path.of("TEST_DIRECTORY").removeDirectoryEntries() }
    }

    @Test
    fun `Produces IOException if attempting to remove directory entries for a resource, that is not a directory`() {
        val res = venv.create("TEST_FILE.txt")
        assertThrows<IOException> { res.removeDirectoryEntries() }
    }

    @Test
    fun `Removes directory entries recursively`() {
        val dir = venv.createDirectory("TEST_DIRECTORY_A")
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_A.txt")
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_B.txt")
        venv.createDirectory("TEST_DIRECTORY_A", "TEST_DIRECTORY_B", "TEST_FILE_C.txt")
        venv.create("TEST_DIRECTORY_A", "TEST_DIRECTORY_B", "TEST_FILE_D.txt")
        venv.create("TEST_DIRECTORY_A", "TEST_DIRECTORY_B", "TEST_FILE_E.txt")
        dir.removeDirectoryEntries()
        assertEquals(0, dir.getDirectoryEntries().size)
    }

    @Test
    fun `Produces IOException if attempting to remove resource, that does not exist`() {
        assertThrows<IOException> { Path.of("TEST_DIRECTORY").remove() }
    }

    @Test
    fun `Removes resource`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        venv.create("TEST_DIRECTORY", "TEST_FILE_A.txt")
        val res = venv.create("TEST_FILE.txt")
        assertTrue(dir.exists())
        assertTrue(res.exists())
        dir.remove()
        res.remove()
        assertFalse(dir.exists())
        assertFalse(res.exists())
    }

    @Test
    fun `Produces IOException if attempting to retrieve resource size in bytes, that does not exist`() {
        assertThrows<IOException> { Path.of("TEST_FILE.txt").getSizeInBytes() }
    }

    @Test
    fun `Gets size of 0 bytes for an empty directory`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        assertEquals(0, dir.getSizeInBytes())
    }

    @Test
    fun `Gets size of resource in bytes`() {
        val dir = venv.createDirectory("TEST_DIRECTORY_A")
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_A.txt").writeBytes(byteArrayOf(1, 2, 3))
        venv.createDirectory("TEST_DIRECTORY_A", "TEST_DIRECTORY_B")
        venv.create("TEST_DIRECTORY_A", "TEST_DIRECTORY_B", "TEST_FILE_B.txt").writeBytes(byteArrayOf(4, 5, 6))
        assertEquals(6, dir.getSizeInBytes())
    }

    @Test
    fun `Produces IOException if attempting to calculate resource size in specified capacity type, that does not exist`() {
        assertThrows<IOException> { Path.of("TEST_FILE.txt").getSize(capacityType = CapacityType.BYTE) }
    }

    @Test
    fun `Calculates size in specified capacity type`() {
        val dir = venv.createDirectory("TEST_DIRECTORY_A")
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_A.txt").writeBytes((0..2047).map { it.toByte() }.toByteArray()) // 2048 bytes
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_B.txt").writeBytes((0..511).map { it.toByte() }.toByteArray()) // 512 bytes
        val size = dir.getSize(capacityType = CapacityType.KILOBYTE)
        assertEquals(BigDecimal(2.50).setScale(2), size)
    }

    @Test
    fun `Produces IOException if attempting to detect content type of resource, that does not exist`() {
        assertThrows<IOException> { Path.of("TEST_FILE.txt").getContentType() }
    }

    @Test
    fun `Content type is not detected for directory`() {
        assertEquals(null, venv.createDirectory("TEST_DIRECTORY").getContentType())
    }

    @Test
    fun `Gets content type of resource`() {
        assertEquals("text/plain", venv.create("TEST_FILE_A.txt").getContentType())
        assertEquals("application/json", venv.create("TEST_FILE_B.json").getContentType())
    }
}