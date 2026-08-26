package me.xdan.prism.compiler

import java.nio.ByteBuffer
import java.nio.ByteOrder

class ResTableEditor {

    fun patchPackageName(input: ByteArray, newPackage: String): ByteArray {
        val buffer = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN)
        
        // 1. Read Header
        val type = buffer.short.toInt() and 0xFFFF
        if (type != 0x0002) throw Exception("Not a valid resources.arsc file")
        val headerSize = buffer.short.toInt() and 0xFFFF
        val fileSize = buffer.int
        val packageCount = buffer.int
        
        // 2. Skip Global String Pool
        buffer.position(headerSize)
        val spType = buffer.short.toInt() and 0xFFFF
        if (spType != 0x0001) throw Exception("Expected Global String Pool")
        buffer.short // headerSize
        val spSize = buffer.int
        buffer.position(headerSize + spSize)
        
        // 3. Find Package Chunk
        // There can be multiple packages, but usually only one in base.apk
        for (i in 0 until packageCount) {
            val pStart = buffer.position()
            val pType = buffer.short.toInt() and 0xFFFF
            if (pType != 0x0200) throw Exception("Expected Package chunk at $pStart")
            
            val pHeaderSize = buffer.short.toInt() and 0xFFFF
            val pSize = buffer.int
            val pId = buffer.int
            
            // Package Name starts at pStart + 12
            // It's 128 UTF-16 characters (256 bytes)
            val namePos = pStart + 12
            buffer.position(namePos)
            
            val nameBytes = newPackage.toByteArray(Charsets.UTF_16LE)
            val patchBytes = ByteArray(256)
            System.arraycopy(nameBytes, 0, patchBytes, 0, minOf(nameBytes.size, 256))
            
            // Write back to the buffer
            for (j in 0 until 256) {
                input[namePos + j] = patchBytes[j]
            }
            
            buffer.position(pStart + pSize)
        }
        
        return input
    }
}
