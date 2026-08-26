package me.xdan.prism.compiler

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryXmlEditor {

    fun patchPackageName(input: ByteArray, oldPackageHint: String?, newPackage: String): ByteArray {
        val buffer = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN)
        
        // 1. Read AXML Header
        val magic = buffer.int
        if (magic != 0x00080003) throw Exception("Not a valid AXML file")
        buffer.int // fileSize
        
        // 2. Find String Pool Chunk
        val chunkType = buffer.short.toInt() and 0xFFFF
        if (chunkType != 0x0001) throw Exception("Expected String Pool chunk")
        
        buffer.short // headerSize
        val chunkSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringStart = buffer.int
        buffer.int // stylesStart
        
        val isUtf8 = (flags and (1 shl 8)) != 0
        
        // Read Offsets
        val offsets = IntArray(stringCount) { buffer.int }
        
        // Read Strings
        val strings = mutableListOf<String>()
        val stringDataStart = buffer.position() - (28 + stringCount * 4) + stringStart
        
        for (i in 0 until stringCount) {
            val offset = offsets[i]
            buffer.position(stringDataStart + offset)
            val (encodedLen, decodedLen) = if (isUtf8) {
                // Read encoded length
                var eLen = buffer.get().toInt() and 0xFF
                if ((eLen and 0x80) != 0) {
                    eLen = (eLen and 0x7F shl 8) or (buffer.get().toInt() and 0xFF)
                }
                // Read decoded length
                var dLen = buffer.get().toInt() and 0xFF
                if ((dLen and 0x80) != 0) {
                    dLen = (dLen and 0x7F shl 8) or (buffer.get().toInt() and 0xFF)
                }
                Pair(eLen, dLen)
            } else {
                var dLen = buffer.short.toInt() and 0xFFFF
                if ((dLen and 0x8000) != 0) {
                    dLen = (dLen and 0x7FFF shl 16) or (buffer.short.toInt() and 0xFFFF)
                }
                Pair(dLen * 2, dLen)
            }
            val bytes = ByteArray(encodedLen)
            buffer.get(bytes)
            strings.add(String(bytes, if (isUtf8) Charsets.UTF_8 else Charsets.UTF_16LE))
        }

        // 3. Find the old package name if not provided
        var oldPackage = oldPackageHint
        if (oldPackage == null) {
            oldPackage = strings.find { it.contains(".") && !it.startsWith("http") && !it.contains("/") }
        }
        
        // 4. Patch Strings
        var patched = false
        for (i in strings.indices) {
            if (strings[i] == oldPackage) {
                strings[i] = newPackage
                patched = true
            }
        }
        
        if (!patched) return input
        
        // 5. Rebuild String Pool
        val newStringData = mutableListOf<ByteArray>()
        val newOffsets = IntArray(stringCount)
        var currentOffset = 0
        
        for (i in 0 until stringCount) {
            newOffsets[i] = currentOffset
            val s = strings[i]
            val bytes = s.toByteArray(if (isUtf8) Charsets.UTF_8 else Charsets.UTF_16LE)
            val out = mutableListOf<Byte>()
            if (isUtf8) {
                val len = s.length
                val encodedLen = bytes.size
                if (encodedLen > 0x7F) {
                    out.add(((encodedLen shr 8) or 0x80).toByte())
                    out.add((encodedLen and 0xFF).toByte())
                } else {
                    out.add(encodedLen.toByte())
                }
                if (len > 0x7F) {
                    out.add(((len shr 8) or 0x80).toByte())
                    out.add((len and 0xFF).toByte())
                } else {
                    out.add(len.toByte())
                }
                bytes.forEach { out.add(it) }
                out.add(0)
            } else {
                val len = s.length
                if (len > 0x7FFF) {
                    val high = (len shr 16) or 0x8000
                    val low = len and 0xFFFF
                    out.add((high and 0xFF).toByte())
                    out.add((high shr 8).toByte())
                    out.add((low and 0xFF).toByte())
                    out.add((low shr 8).toByte())
                } else {
                    out.add((len and 0xFF).toByte())
                    out.add((len shr 8).toByte())
                }
                bytes.forEach { out.add(it) }
                out.add(0)
                out.add(0)
            }
            val entry = out.toByteArray()
            newStringData.add(entry)
            currentOffset += entry.size
        }
        
        val totalStringDataSize = currentOffset
        val padding = (4 - totalStringDataSize % 4) % 4
        
        val newChunkSize = 28 + stringCount * 4 + totalStringDataSize + padding
        val sizeDiff = newChunkSize - chunkSize
        
        val result = ByteBuffer.allocate(input.size + sizeDiff).order(ByteOrder.LITTLE_ENDIAN)
        result.putInt(0x00080003)
        result.putInt(input.size + sizeDiff)
        
        result.putShort(0x0001.toShort())
        result.putShort(28.toShort())
        result.putInt(newChunkSize)
        result.putInt(stringCount)
        result.putInt(styleCount)
        result.putInt(flags)
        result.putInt(28 + stringCount * 4)
        result.putInt(0)
        
        for (off in newOffsets) result.putInt(off)
        for (data in newStringData) result.put(data)
        repeat(padding) { result.put(0.toByte()) }
        
        buffer.position(8 + chunkSize)
        result.put(buffer)
        
        return result.array()
    }
}
