package me.xdan.prism.compiler

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryXmlEditor {

    fun patchPackageName(input: ByteArray, oldPackageHint: String?, newPackage: String): ByteArray =
        if (oldPackageHint.isNullOrBlank()) {
            input
        } else {
            patchStrings(input, mapOf(oldPackageHint to newPackage))
        }

    fun patchStrings(input: ByteArray, replacements: Map<String, String>): ByteArray {
        if (replacements.isEmpty()) return input

        val buffer = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.int
        if (magic != 0x00080003) throw IllegalArgumentException("Not a valid AXML file")
        buffer.int

        val chunkType = buffer.short.toInt() and 0xFFFF
        if (chunkType != 0x0001) throw IllegalArgumentException("Expected string pool chunk")
        buffer.short
        val chunkSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringStart = buffer.int
        buffer.int

        val isUtf8 = (flags and (1 shl 8)) != 0
        val offsets = IntArray(stringCount) { buffer.int }
        val stringDataStart = 8 + stringStart
        val strings = ArrayList<String>(stringCount)

        for (i in 0 until stringCount) {
            buffer.position(stringDataStart + offsets[i])
            if (isUtf8) {
                readUtf8Length(buffer)
                val decodedLength = readUtf8Length(buffer)
                val bytes = ByteArray(decodedLength)
                buffer.get(bytes)
                strings += String(bytes, Charsets.UTF_8)
            } else {
                val length = readUtf16Length(buffer)
                val bytes = ByteArray(length * 2)
                buffer.get(bytes)
                strings += String(bytes, Charsets.UTF_16LE)
            }
        }

        var changed = false
        for (i in strings.indices) {
            replacements[strings[i]]?.let { replacement ->
                if (replacement != strings[i]) {
                    strings[i] = replacement
                    changed = true
                }
            }
        }
        if (!changed) return input

        val newStringData = ArrayList<ByteArray>(stringCount)
        val newOffsets = IntArray(stringCount)
        var currentOffset = 0
        for (i in strings.indices) {
            newOffsets[i] = currentOffset
            val data = if (isUtf8) encodeUtf8(strings[i]) else encodeUtf16(strings[i])
            newStringData += data
            currentOffset += data.size
        }

        val padding = (4 - currentOffset % 4) % 4
        val newChunkSize = 28 + stringCount * 4 + currentOffset + padding
        val sizeDiff = newChunkSize - chunkSize
        val result = ByteBuffer.allocate(input.size + sizeDiff).order(ByteOrder.LITTLE_ENDIAN)
        result.putInt(0x00080003)
        result.putInt(input.size + sizeDiff)
        result.putShort(0x0001)
        result.putShort(28)
        result.putInt(newChunkSize)
        result.putInt(stringCount)
        result.putInt(styleCount)
        result.putInt(flags)
        result.putInt(28 + stringCount * 4)
        result.putInt(0)
        newOffsets.forEach(result::putInt)
        newStringData.forEach(result::put)
        repeat(padding) { result.put(0) }

        buffer.position(8 + chunkSize)
        result.put(buffer)
        return result.array()
    }

    private fun readUtf8Length(buffer: ByteBuffer): Int {
        val first = buffer.get().toInt() and 0xFF
        return if ((first and 0x80) == 0) {
            first
        } else {
            ((first and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
        }
    }

    private fun readUtf16Length(buffer: ByteBuffer): Int {
        val first = buffer.short.toInt() and 0xFFFF
        return if ((first and 0x8000) == 0) {
            first
        } else {
            ((first and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
        }
    }

    private fun encodeUtf8(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val decodedLength = value.length
        val output = ArrayList<Byte>(bytes.size + 4)
        writeUtf8Length(output, bytes.size)
        writeUtf8Length(output, decodedLength)
        bytes.forEach(output::add)
        output.add(0)
        return output.toByteArray()
    }

    private fun encodeUtf16(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_16LE)
        val output = ArrayList<Byte>(bytes.size + 4)
        val length = value.length
        if (length > 0x7FFF) {
            output.add((((length shr 16) or 0x8000) and 0xFF).toByte())
            output.add((((length shr 16) or 0x8000) shr 8).toByte())
            output.add((length and 0xFF).toByte())
            output.add((length shr 8).toByte())
        } else {
            output.add((length and 0xFF).toByte())
            output.add((length shr 8).toByte())
        }
        bytes.forEach(output::add)
        output.add(0)
        output.add(0)
        return output.toByteArray()
    }

    private fun writeUtf8Length(output: MutableList<Byte>, length: Int) {
        if (length > 0x7F) {
            output.add(((length shr 8) or 0x80).toByte())
            output.add((length and 0xFF).toByte())
        } else {
            output.add(length.toByte())
        }
    }
}
