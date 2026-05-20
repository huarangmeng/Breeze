package com.hrm.breeze.data.rag

import kotlin.math.sqrt

object RagVectorCodec {
    fun encode(vector: FloatArray): ByteArray {
        val bytes = ByteArray(vector.size * Float.SIZE_BYTES)
        vector.forEachIndexed { index, value ->
            val bits = value.toRawBits()
            val offset = index * Float.SIZE_BYTES
            bytes[offset] = (bits and 0xFF).toByte()
            bytes[offset + 1] = ((bits ushr 8) and 0xFF).toByte()
            bytes[offset + 2] = ((bits ushr 16) and 0xFF).toByte()
            bytes[offset + 3] = ((bits ushr 24) and 0xFF).toByte()
        }
        return bytes
    }

    fun decode(bytes: ByteArray): FloatArray {
        require(bytes.size % Float.SIZE_BYTES == 0) { "Invalid vector byte size: ${bytes.size}" }
        return FloatArray(bytes.size / Float.SIZE_BYTES) { index ->
            val offset = index * Float.SIZE_BYTES
            val bits =
                (bytes[offset].toInt() and 0xFF) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 3].toInt() and 0xFF) shl 24)
            Float.fromBits(bits)
        }
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        var dot = 0f
        var aNorm = 0f
        var bNorm = 0f
        for (index in a.indices) {
            dot += a[index] * b[index]
            aNorm += a[index] * a[index]
            bNorm += b[index] * b[index]
        }
        val denominator = sqrt(aNorm.toDouble()) * sqrt(bNorm.toDouble())
        return if (denominator == 0.0) 0f else (dot / denominator).toFloat()
    }
}
