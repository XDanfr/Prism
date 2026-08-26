package me.xdan.prism.compiler

import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ZipAligner {

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var count: Long = 0
            private set

        override fun write(b: Int) {
            out.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len.toLong()
        }
    }

    fun align(inputApk: File, outputApk: File, alignment: Int = 4) {
        val zipFile = ZipFile(inputApk)
        val cos = CountingOutputStream(FileOutputStream(outputApk))
        val zos = ZipOutputStream(cos)
        
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name
            val nameBytes = entryName.toByteArray(Charsets.UTF_8)
            
            val newEntry = ZipEntry(entryName)
            newEntry.method = entry.method
            newEntry.time = entry.time
            
            if (entry.method == ZipEntry.STORED) {
                newEntry.size = entry.size
                newEntry.compressedSize = entry.compressedSize
                newEntry.crc = entry.crc
                
                // Calculate where the data will start.
                // ZipOutputStream.putNextEntry writes the local header.
                // Local header: 30 bytes + name length + extra length.
                val currentOffset = cos.count
                val localHeaderBaseSize = 30 + nameBytes.size
                val padding = (alignment - (currentOffset + localHeaderBaseSize) % alignment) % alignment
                if (padding > 0) {
                    newEntry.extra = ByteArray(padding.toInt())
                }
            }

            zos.putNextEntry(newEntry)
            zipFile.getInputStream(entry).use { it.copyTo(zos) }
            zos.closeEntry()
        }
        zipFile.close()
        zos.close()
    }
}
