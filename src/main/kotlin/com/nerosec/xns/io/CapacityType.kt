package com.nerosec.xns.io

enum class CapacityType(val bytes: Long) {
    BYTE(1),
    KILOBYTE(1024),
    MEGABYTE(KILOBYTE.bytes * 1024),
    GIGABYTE(MEGABYTE.bytes * 1024),
    TERABYTE(GIGABYTE.bytes * 1024),
    PETABYTE(TERABYTE.bytes * 1024)
}