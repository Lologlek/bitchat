package bitchat

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.DataFormatException

object CompressionUtil {
    const val COMPRESSION_THRESHOLD = 100

    fun shouldCompress(data: ByteArray): Boolean {
        if (data.size < COMPRESSION_THRESHOLD) return false
        val unique = mutableSetOf<Byte>()
        val limit = minOf(data.size, 256)
        for (i in 0 until limit) {
            unique.add(data[i])
        }
        val ratio = unique.size.toDouble() / limit.toDouble()
        return ratio < 0.9
    }

    fun compress(data: ByteArray): ByteArray? {
        if (data.size < COMPRESSION_THRESHOLD) return null
        val deflater = Deflater(Deflater.BEST_SPEED)
        deflater.setInput(data)
        deflater.finish()
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        deflater.end()
        val compressed = output.toByteArray()
        return if (compressed.isNotEmpty() && compressed.size < data.size) compressed else null
    }

    fun decompress(compressed: ByteArray, originalSize: Int): ByteArray? {
        val inflater = Inflater()
        inflater.setInput(compressed)
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        return try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                output.write(buffer, 0, count)
                if (output.size() > originalSize) {
                    inflater.end()
                    return null
                }
            }
            inflater.end()
            val result = output.toByteArray()
            if (result.size == originalSize) result else null
        } catch (e: DataFormatException) {
            inflater.end()
            null
        }
    }
}
