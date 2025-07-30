package com.nerosec.xns.io.extensions

import com.nerosec.xns.io.CapacityType
import com.nerosec.xns.io.ListOption
import org.apache.tika.Tika
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries

object PathExtensions {

    /**
     * Recursively lists directory entries, based on [depth] and [options] specified.
     * @param depth level of depth to recursively scan directory for.
     * @param options options for listing directory entries. See [com.nerosec.xns.io.ListOption].
     * @throws java.io.IOException if resource does not exist, or it is not a directory.
     * @return list of entries found,
     */
    fun Path.getDirectoryEntries(depth: Int = 1, vararg options: ListOption = arrayOf(ListOption.INCLUDE_HIDDEN_FILES)): List<Path> {
        if (!exists()) throw IOException("'$this' could not be found.")
        if (!isDirectory()) throw IOException("'$this' is not a directory.")
        if (depth < 1) return emptyList()
        val entries = ArrayList<Path>()
        listDirectoryEntries()
            .filter { !it.isHidden() || options.contains(ListOption.INCLUDE_HIDDEN_FILES) }
            .filter { !it.isSymbolicLink() || options.contains(ListOption.INCLUDE_SYMBOLIC_LINKS) }
            .forEach { entry ->
                entries.add(entry)
                if (entry.isDirectory()) {
                    entry.getDirectoryEntries(depth.dec(), *options).forEach {
                        entries.add(it)
                    }
                }
            }
        return entries
    }

    /**
     * Removes directory contents.
     * @throws IOException if resource does not exist, or it is not a directory.
     */
    fun Path.removeDirectoryEntries() = getDirectoryEntries(Int.MAX_VALUE, *ListOption.entries.toTypedArray())
        .reversed()
        .forEach { Files.delete(it) }

    /**
     * Removes resource.
     * @throws IOException if resource does not exist.
     */
    fun Path.remove() {
        if (!exists()) throw IOException("'$this' could not be found.")
        if (isDirectory()) removeDirectoryEntries()
        Files.delete(this)
    }

    /**
     * Gets resource size in bytes.
     * @return resource size in bytes.
     * @throws IOException if resource does not exist.
     */
    fun Path.getSizeInBytes(): Long {
        if (!exists()) throw IOException("'$this' could not be found.")
        return if (isDirectory()) {
            getDirectoryEntries(Int.MAX_VALUE).sumOf {
                if (it.isDirectory()) 0L else it.getSizeInBytes()
            }
        } else {
            Files.size(this)
        }
    }

    /**
     * Calculates size in specified [capacityType] and [scale].
     * @param capacityType type of capacity to calculate size by. See [com.nerosec.xns.io.CapacityType].
     * @param scale number of decimal points for the result.
     * @return size in specified [capacityType] and [scale].
     * @throws IOException if resource does not exist.
     */
    fun Path.getSize(capacityType: CapacityType, scale: Int = 2): BigDecimal =
        BigDecimal(getSizeInBytes()).divide(BigDecimal(capacityType.bytes), scale, RoundingMode.HALF_UP)

    /**
     * Tries to detect content type of resource. Directories do not have content type.
     * @return content type of resource if it can be detected.
     * @throws IOException if resource does not exist.
     */
    fun Path.getContentType(): String? {
        if (!exists()) throw IOException("'$this' could not be found.")
        return if (isDirectory()) null else Tika().detect(this)
    }
}