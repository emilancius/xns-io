package com.nerosec.xns.io.extensions

import com.nerosec.xns.io.CapacityType
import com.nerosec.xns.io.HashType
import com.nerosec.xns.io.ListOption
import com.nerosec.xns.io.extensions.ByteArrayExtensions.toHexString
import org.apache.tika.Tika
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

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

    /**
     * Calculates resource content hash, based on [hashType] specified. Content hash cannot be calculated for directories.
     * @param hashType type of hash to produce.
     * @return resource content has as hex string.
     * @throws IOException if resource does not exist.
     */
    fun Path.getContentHash(hashType: HashType = HashType.SHA_256): String? {
        if (!exists()) throw IOException("'$this' could not be found.")
        if (isDirectory()) return null
        val digest = MessageDigest.getInstance(hashType.value)
        inputStream().use { inputStream ->
            val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = inputStream.read(byteArray, 0, DEFAULT_BUFFER_SIZE)
            for (i in generateSequence(0) { it }) {
                if (read > -1) {
                    digest.update(byteArray, 0, read)
                    read = inputStream.read(byteArray, 0, DEFAULT_BUFFER_SIZE)
                } else {
                    break
                }
            }
        }
        return digest.digest().toHexString()
    }

    /**
     * Creates resource from specified [inputStream].
     * @return path of created resource.
     * @throws IOException if resource by the same name exists, or if resource's parent directory does not exist.
     */
    fun Path.create(inputStream: InputStream): Path {
        if (exists()) throw IOException("'$this' could not be created: '$this' exists.")
        if (!parent.exists()) throw IOException("'$this' could not be created: parent directory '$parent' could not be found.")
        Files.copy(inputStream, this)
        return this
    }

    /**
     * Copies resource as specified [path]. If resource is a directory, its contents are copied recursively.
     * @param path to copy the resource as.
     * @param options copy options. See [StandardCopyOption].
     * @return copy of the resource.
     * @throws IOException if resource does not exist, parent directory of [path] does not exist or
     * [path] exists and [StandardCopyOption.REPLACE_EXISTING] is not specified.
     */
    fun Path.copyAs(path: Path, vararg options: CopyOption = emptyArray<CopyOption>()): Path {
        if (!exists()) throw IOException("'$this' could not be found.")
        if (!path.parent.exists()) throw IOException("'$this' could not be copied as '$path': parent directory '${path.parent}' could not be found.")
        if (StandardCopyOption.REPLACE_EXISTING !in options && path.exists()) throw IOException("'$this' cannot be copied as '$path': '$path' exists.")
        Files.copy(this, path, *options)
        if (isDirectory()) {
            getDirectoryEntries().forEach {
                Files.copy(it, path.resolve(this.relativize(it)), *options)
            }
        }
        return path
    }

    /**
     * Copies resource to specified [directory]. If resource is a directory, its contents are copied recursively.
     * @param directory to copy the resource to.
     * @param options copy options. See [StandardCopyOption].
     * @return copy of the resource.
     * @throws IOException if resource does not exist, [directory] does not exist or [directory] is not a directory.
     */
    fun Path.copyTo(directory: Path, vararg options: CopyOption = emptyArray<CopyOption>()): Path {
        if (!exists()) throw IOException("'$this' could not be found.")
        if (!directory.exists()) throw IOException("'$this' could not be found.")
        if (!directory.isDirectory()) throw IOException("'$this' cannot be copied to '$directory': '$this' is not a directory.")
        return copyAs(directory.resolve(this.name), *options)
    }

    /**
     * Moves resource as specified [path]. If resource is a directory, it and its contents are copied and removed at source location.
     * @param path to move the resource as.
     * @param options move options. See [StandardCopyOption].
     * @return moved resource.
     * @throws IOException if resource does not exist or [path] exists [StandardCopyOption.REPLACE_EXISTING] is not specified.
     */
    fun Path.moveAs(path: Path, vararg options: CopyOption = emptyArray<CopyOption>()): Path {
        if (!exists()) throw IOException("'$this' could not be found.")
        if (StandardCopyOption.REPLACE_EXISTING !in options && path.exists()) throw IOException("'$this' cannot be moved as '$path': '$path' exists.")
        if (!path.parent.exists()) throw IOException("'$this' could not be moved as '$path': parent directory '${path.parent}' could not be found.")
        return if (isDirectory()) {
            val copy = copyAs(path)
            remove()
            copy
        } else {
            Files.move(this, path, *options)
        }
    }

    /**
     * Moves resource to specified [directory]. If resource is a directory, its contents are copied and removed at source location.
     * @param directory to move resource to.
     * @param options move options. See [StandardCopyOption].
     * @return moved resource.
     * @throws IOException if resource does not exist, [directory] does not exist or [directory] is not a directory.
     */
    fun Path.moveTo(directory: Path, vararg options: CopyOption = emptyArray<CopyOption>()): Path {
        if (!exists()) throw IOException("'$this' could not be found.")
        if (!directory.exists()) throw IOException("'$this' could not be found.")
        if (!directory.isDirectory()) throw IOException("'$this' cannot be moved to '$directory': '$this' is not a directory.")
        return moveAs(directory.resolve(this.name), *options)
    }

    /**
     * Renames resource to [name].
     * @param name updated name of the resource.
     * @return renamed resource.
     * @throws IOException if resource does not exist or resource by the [name] exists.
     */
    fun Path.renameTo(name: String): Path {
        if (!exists()) throw IOException("'$this' could not be found.")
        val target = parent.resolve(name)
        if (target.exists()) throw IOException("'$this' could not be renamed to '$name': '$target' exists.")
        return Files.move(this, target)
    }
}