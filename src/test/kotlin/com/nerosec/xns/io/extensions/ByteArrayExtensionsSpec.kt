package com.nerosec.xns.io.extensions

import com.nerosec.xns.io.extensions.ByteArrayExtensions.toHexString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ByteArrayExtensionsSpec {

    @Test
    fun `Converts ByteArray to hex string`() {
        // "TEST" == "54455354"
        val byteArray = "TEST".toByteArray()
        val hexString = byteArray.toHexString()
        assertEquals("54455354", hexString)
    }
}