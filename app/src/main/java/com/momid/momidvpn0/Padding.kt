package com.momid.momidvpn0

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.random.Random

val offset = ByteArray(4) {
    383.toByte()
}

fun byteArrayToInt(byteArray: ByteArray): Int {
    require(byteArray.size == 4) { "ByteArray must be of size 4" }
    return (byteArray[0].toInt() shl 24) or
            (byteArray[1].toInt() and 0xFF shl 16) or
            (byteArray[2].toInt() and 0xFF shl 8) or
            (byteArray[3].toInt() and 0xFF)
}

fun intToByteArray(value: Int): ByteArray {
    return byteArrayOf(
        (value shr 24).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value and 0xFF).toByte()
    )
}

fun offset(byteArray: ByteArray): ByteArray {
    val size = intToByteArray(byteArray.size)
    val random = Random.nextInt(83, 183)
    val packet = ByteArray(random) {
        if (it < 4) {
            size[it]
        } else if (it < 4 + byteArray.size) {
            byteArray[it - 4]
        } else {
            383.toByte()
        }
    }
    return packet
}

fun unOffset(byteArray: ByteArray): ByteArray {
    val size = byteArrayToInt(byteArray.sliceArray(0 until 4))
    val actual = byteArray.sliceArray(4 until 4 + size)
    return actual
}

fun isOffset(byteArray: ByteArray): Boolean {
    return byteArray.sliceArray(byteArray.size - 4 until byteArray.size).contentEquals(offset)
}

fun ByteArray.toByteBuff(): ByteBuf {
    return Unpooled.wrappedBuffer(this)
}

fun ipText(byteArray: ByteArray): String {
    return byteArray.joinToString(".") {
        "" + (it.toInt() and 0xff)
    }
}

fun ByteArray.offsetize(): ByteArray {
    if (this.size < 83) {
        return offset(this)
    } else {
        return this
    }
}

fun ByteArray.unoffsetize(): ByteArray {
    if (isOffset(this)) {
        return unOffset(this)
    } else {
        return this
    }
}
