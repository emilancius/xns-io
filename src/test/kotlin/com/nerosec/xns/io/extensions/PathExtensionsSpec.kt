package com.nerosec.xns.io.extensions

import com.nerosec.xns.io.CapacityType
import com.nerosec.xns.io.HashType
import com.nerosec.xns.io.ListOption
import com.nerosec.xns.io.Venv
import com.nerosec.xns.io.extensions.PathExtensions.copyAs
import com.nerosec.xns.io.extensions.PathExtensions.copyTo
import com.nerosec.xns.io.extensions.PathExtensions.create
import com.nerosec.xns.io.extensions.PathExtensions.getContentHash
import com.nerosec.xns.io.extensions.PathExtensions.getContentType
import com.nerosec.xns.io.extensions.PathExtensions.getDirectoryEntries
import com.nerosec.xns.io.extensions.PathExtensions.getSize
import com.nerosec.xns.io.extensions.PathExtensions.getSizeInBytes
import com.nerosec.xns.io.extensions.PathExtensions.moveAs
import com.nerosec.xns.io.extensions.PathExtensions.moveTo
import com.nerosec.xns.io.extensions.PathExtensions.remove
import com.nerosec.xns.io.extensions.PathExtensions.removeDirectoryEntries
import com.nerosec.xns.io.extensions.PathExtensions.renameTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

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
    fun `Given, that resource does not exist, produces IOException on attempt to retrieve directory entries`() {
        assertThrows<IOException> { Path.of("TEST_DIRECTORY").getDirectoryEntries() }
    }

    @Test
    fun `Given, that resource is not a directory, produces IOException on attempt to retrieve directory entries`() {
        val dir = venv.create("TEST_FILE.txt")
        assertThrows<IOException> { dir.getDirectoryEntries() }
    }

    @Test
    fun `Given, that specified depth is less than 1, finds no entries on attempt to retrieve directory entries`() {
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
    fun `Given, that resource does not exist, produces IOException on attempt to remove directory entries`() {
        assertThrows<IOException> { Path.of("TEST_DIRECTORY").removeDirectoryEntries() }
    }

    @Test
    fun `Given, that resource is not a directory, produces IOException on attempt to remove directory entries`() {
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
    fun `Given, that resource does not exist, produces IOException on attempt to remove resource`() {
        assertThrows<IOException> { Path.of("TEST_DIRECTORY").remove() }
    }

    @Test
    fun `Removes resource`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        venv.create("TEST_DIRECTORY", "TEST_FILE_A.txt")
        val res = venv.create("TEST_FILE.txt")
        assertTrue { dir.exists() }
        assertTrue { res.exists() }
        dir.remove()
        res.remove()
        assertFalse { dir.exists() }
        assertFalse { res.exists() }
    }

    @Test
    fun `Given, that resource does not exist, produces IOException on attempt to calculate resource size in bytes`() {
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
    fun `Given, that resource does not exist, produces IOException on attempt to calculate resource size in specified capacity type`() {
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
    fun `Given, that resource does not exist, produces IOException on attempt to detect resource content type`() {
        assertThrows<IOException> { Path.of("TEST_FILE.txt").getContentType() }
    }

    @Test
    fun `Given, that resource is a directory, content type cannot be detected`() {
        assertEquals(null, venv.createDirectory("TEST_DIRECTORY").getContentType())
    }

    @Test
    fun `Gets content type of resource`() {
        assertEquals("text/plain", venv.create("TEST_FILE_A.txt").getContentType())
        assertEquals("application/json", venv.create("TEST_FILE_B.json").getContentType())
    }

    @Test
    fun `Given, that resource does not exist, produces IOException on attempt to calculate its content hash`() {
        assertThrows<IOException> { Path.of("TEST_FILE.txt").getContentHash() }
    }

    @Test
    fun `Given, that resource is a directory, content hash cannot be calculated`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        assertEquals(null, dir.getContentHash())
    }

    @Test
    fun `Calculates content hash for a resource`() {
        val res = venv.create("TEST_FILE.txt")
        val contentHashSha256 = res.getContentHash()!!
        val contentHashSha512 = res.getContentHash(hashType = HashType.SHA_512)!!
        assertFalse { contentHashSha256 == contentHashSha512 }
        assertEquals(64, contentHashSha256.length)
        assertEquals(128, contentHashSha512.length)
        res.writeBytes(byteArrayOf(1)) // Change content.
        assertFalse { contentHashSha256 == res.getContentHash() }
    }

    @Test
    fun `Given, that resource does not exist, produces IOException on attempt to create a resource`() {
        val res = venv.create("TEST_FILE.txt")
        val content = "TEST_FILE.txt content."
        assertThrows<IOException> {
            content.byteInputStream().use { inputStream ->
                res.create(inputStream)
            }
        }
    }

    @Test
    fun `Given, that resource parent directory does not exist, produces IOException on attempt to create a resource`() {
        val content = "TEST_FILE.txt content"
        assertThrows<IOException> {
            content.byteInputStream().use { inputStream ->
                Path.of("TEST_DIRECTORY${File.separator}TEST_FILE.txt").create(inputStream)
            }
        }
    }

    @Test
    fun `Creates a resource from an input stream`() {
        val content = "TEST_FILE.txt content"
        var res = Path.of("venv${File.separator}TEST_FILE.txt")
        content.byteInputStream().use { inputStream ->
            res = res.create(inputStream)
        }
        assertTrue { res.exists() }
        assertEquals(content, res.readText())
    }

    @Test
    fun `Given, that source resource does not exist, produces IOException on attempt to copy a resource`() {
        assertThrows<IOException> { Path.of("TEST_FILE_A.txt").copyAs(Path.of("TEST_FILE_B.txt")) }
    }

    @Test
    fun `Given, that parent directory for target resource does not exists, produces IOException on attempt to copy a resource`() {
        val res = venv.create("TEST_FILE_A.txt")
        assertThrows<IOException> { res.copyAs(Path.of("TEST_DIRECTORY${File.separator}TEST_FILE_B.txt")) }
    }

    @Test
    fun `Given, that target resource exists and StandardCopyOption#REPLACE_EXISTING is not specified, produces IOException on attempt to copy a resource`() {
        val res = venv.create("TEST_FILE_A.txt")
        venv.create("TEST_FILE_B.txt")
        assertThrows<IOException> { res.copyAs(Path.of("venv${File.separator}TEST_FILE_B.txt")) }
    }

    @Test
    fun `Given, that target resource exists and StandardCopyOption#REPLACE_EXISTING is specified, copies source resource as target resource and replaces existing one`() {
        val resA = venv.create("TEST_FILE_A.txt")
        val content = "TEST_FILE_A.txt content"
        resA.writeText(content)
        val resB = venv.create("TEST_FILE_B.txt")
        assertEquals("", resB.readText())
        val copy = resA.copyAs(resB, StandardCopyOption.REPLACE_EXISTING)
        assertEquals(content, copy.readText())
    }

    @Test
    fun `Copies source resource as target resource`() {
        val res = venv.create("TEST_FILE_A.txt")
        val content = "TEST_FILE_A.txt content"
        res.writeText(content)
        var copy = res.copyAs(Path.of("venv${File.separator}TEST_FILE_B.txt"))
        assertTrue { copy.exists() }
        assertEquals(content, copy.readText())
        val dir = venv.createDirectory("TEST_DIRECTORY_A")
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_C.txt")
        copy = dir.copyAs(Path.of("venv${File.separator}TEST_DIRECTORY_B"))
        assertTrue { copy.exists() }
        assertEquals(1, dir.getDirectoryEntries().size)
    }

    @Test
    fun `Given, that resource does not exist, produces IOException on attempt to copy a resource into a directory`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        assertThrows<IOException> { Path.of("TEST_FILE,txt").copyTo(dir) }
    }

    @Test
    fun `Given, that directory does not exist, produces IOException on attempt to copy a resource into a directory`() {
        val res = venv.create("TEST_FILE.txt")
        assertThrows<IOException> { res.copyTo(Path.of("TEST_DIRECTORY")) }
    }

    @Test
    fun `Given, that directory is not a directory, produces IOException on attempt to copy a resource into a directory`() {
        val resA = venv.create("TEST_FILE_A.txt")
        val resB = venv.create("TEST_FILE_B.txt")
        assertThrows<IOException> { resA.copyTo(resB) }
    }

    @Test
    fun `Copies resource into specified directory`() {
        val res = venv.create("TEST_FILE.txt")
        val content = "TEST_FILE.txt content"
        res.writeText(content)
        val dir = venv.createDirectory("TEST_DIRECTORY")
        val resCopy = res.copyTo(dir)
        assertTrue { res.exists() }
        assertTrue { resCopy.exists() }
        assertEquals(1, dir.getDirectoryEntries().size)
        assertEquals(content, resCopy.readText())
    }

    @Test
    fun `Given, that resource does not exist, produces IOException on attempt to move a resource`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        assertThrows<IOException> { Path.of("TEST_FILE.txt").moveAs(dir.resolve("TEST_FILE.txt")) }
    }

    @Test
    fun `Given, that target resource exists and StandardCopyOption#REPLACE_EXISTING is not specified, produces IOException on attempt to move a resource`() {
        val resA = venv.create("TEST_FILE_A.txt")
        venv.createDirectory("TEST_DIRECTORY")
        val resB = venv.create("TEST_DIRECTORY", "TEST_FILE_B.txt")
        assertThrows<IOException> { resA.moveAs(resB) }
    }

    @Test
    fun `Given, that target resource parent directory does not exist, produces IOException on attempt to move a resource`() {
        val res = venv.create("TEST_FILE.txt")
        assertThrows<IOException> { res.moveAs(Path.of("venv${File.separator}TEST_DIRECTORY${File.separator}TEST_FILE.txt")) }
    }

    @Test
    fun `Given, that target resource exists and StandardCopyOption#REPLACE_EXISTING is specified, moves source resource and replaces existing target resource`() {
        val resA = venv.create("TEST_FILE_A.txt")
        val content = "TEST_FILE_A.txt content"
        resA.writeText(content)
        venv.createDirectory("TEST_DIRECTORY")
        val resB = venv.create("TEST_DIRECTORY", "TEST_FILE_B.txt")
        val resC = resA.moveAs(resB, StandardCopyOption.REPLACE_EXISTING)
        assertFalse { resA.exists() }
        assertTrue { resC.exists() }
        assertEquals(content, resC.readText())
    }

    @Test
    fun `Moves resource`() {
        val dirA = venv.createDirectory("TEST_DIRECTORY_A")
        venv.create("TEST_DIRECTORY_A", "TEST_FILE_A.txt")
        val dirB = venv.createDirectory("TEST_DIRECTORY_B")
        val dirC = dirA.moveAs(dirB.resolve("TEST_DIRECTORY_A"))
        assertFalse { dirA.exists() }
        assertTrue { dirC.exists() }
        assertEquals(1, dirC.getDirectoryEntries().size)
        val resB = venv.create("TEST_FILE_B.txt")
        val resC = resB.moveAs(dirB.resolve("TEST_FILE_B.txt"))
        assertFalse { resB.exists() }
        assertTrue { resC.exists() }
        assertEquals(2, dirB.getDirectoryEntries().size)
    }

    @Test
    fun `Given, that resource does not exist, produces IOException on attempt to move a resource into a directory`() {
        val dir = venv.createDirectory("TEST_DIRECTORY")
        assertThrows<IOException> { Path.of("TEST_FILE.txt").moveTo(dir) }
    }

    @Test
    fun `Given, that directory does not exist, produces IOException on attempt to move a resource into a directory`() {
        val res = venv.create("TEST_FILE.txt")
        assertThrows<IOException> { res.moveTo(Path.of("venv${File.separator}TEST_DIRECTORY")) }
    }

    @Test
    fun `Given, that directory is not a directory, produces IOException on attempt to move a resource into a directory`() {
        val resA = venv.create("TEST_FILE_A.txt")
        val resB = venv.create("TEST_FILE_B.txt")
        assertThrows<IOException> { resA.moveTo(resB) }
    }

    @Test
    fun `Moves resource into a directory`() {
        val resA = venv.create("TEST_FILE.txt")
        val content = "TEST_FILE.txt content"
        resA.writeText(content)
        val dir = venv.createDirectory("TEST_DIRECTORY")
        val resB = resA.moveTo(dir)
        assertFalse { resA.exists() }
        assertTrue { resB.exists() }
        assertEquals(1, dir.getDirectoryEntries().size)
        assertEquals(content, resB.readText())
    }

    @Test
    fun `Given, that resources does not exist, produces IOException on attempt to rename a resource`() {
        assertThrows<IOException> { Path.of("TEST_FILE_A.txt").renameTo("TEST_FILE_B.txt") }
    }

    @Test
    fun `Given, that resource by specified name exists, produces IOException on attempt to rename a resource`() {
        val resA = venv.create("TEST_FILE_A.txt")
        val name = "TEST_FILE_B.txt"
        venv.create(name)
        assertThrows<IOException> { resA.renameTo(name) }
    }

    @Test
    fun `Renamed the resource`() {
        val resA = venv.create("TEST_FILE_A.txt")
        val name = "TEST_FILE_B.txt"
        val resB = resA.renameTo(name)
        assertFalse { resA.exists() }
        assertTrue { resB.exists() }
        assertEquals(name, resB.name)
    }
}