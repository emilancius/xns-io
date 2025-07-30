package com.nerosec.xns.io

import com.nerosec.xns.io.extensions.PathExtensions.remove
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

class Venv {

    companion object {
        const val BASE_DIRECTORY = "venv"
    }

    fun setup() {
        val basedir = getBaseDirectory()
        if (!basedir.exists()) basedir.createDirectories()
    }

    fun cleanup() {
        val basedir = getBaseDirectory()
        if (basedir.exists()) basedir.remove()
    }

    fun createDirectory(vararg parts: String): Path {
        require(!parts.isEmpty()) { "Argument 'parts' cannot be empty." }
        val path = getBaseDirectory().resolve(joinPartsToPath(*parts))
        if (path.exists()) throw IOException("'$path' exists.")
        return path.createDirectories()
    }

    fun create(vararg parts: String): Path {
        require(!parts.isEmpty()) { "Argument 'parts' cannot be empty." }
        val path = getBaseDirectory().resolve(joinPartsToPath(*parts))
        if (path.exists()) throw IOException("'$path' exists.")
        return path.createFile()
    }

    fun remove(path: Path) = path.remove()

    fun remove(path: String) = remove(Path.of(path))

    private fun getBaseDirectory(): Path = Path.of(BASE_DIRECTORY)

    private fun joinPartsToPath(vararg parts: String): Path = Path.of(parts.joinToString(File.separator))
}