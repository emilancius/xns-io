package com.nerosec.xns.io.extensions

object ByteArrayExtensions {

    fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}