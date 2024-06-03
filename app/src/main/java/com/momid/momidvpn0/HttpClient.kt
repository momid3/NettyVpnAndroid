package com.momid.momidvpn0

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import kotlin.system.exitProcess

val HTTP_HEADERS = ("POST /send HTTP/1.1\r\n" +
        "Host: $SERVER_IP_ADDRESS\r\n" +
        "Content-Type: application/octet-stream\r\n" +
        "Content-Length: 30000000\r\n" +
        "\r\n").toByteArray(charset("ASCII"))

val HTTP_RECEIVE_HEADERS = ("GET /receive HTTP/1.1\r\n" +
        "Host: $SERVER_IP_ADDRESS\r\n" +
        "Connection: keep-alive\r\n" +
        "\r\n").toByteArray(charset("ASCII"))

//val sendPackets = Channel<ByteArray>(3000)
//val receivePackets = Channel<ByteArray>(3000)

val sendPackets = ArrayBlockingQueue<ByteArray>(3000)
val receivePackets = ArrayBlockingQueue<ByteArray>(3000)

val client = HttpClient(CIO) {
    install(Logging) {
        level = LogLevel.INFO
    }
}

fun startSendConnection() {
//    runBlocking {
//        client.post("http://" + SERVER_IP_ADDRESS + "/send") {
//            this.setBody(object : OutgoingContent.WriteChannelContent() {
//                override suspend fun writeTo(channel: ByteWriteChannel) {
//                    println("writing")
//                    while (true) {
//                        val packet = sendPackets.receive()
//                        channel.writeFully(packet, 0, packet.size)
//                    }
//                }
//            })
//        }
//    }

    val tcpClient = Socket()
    tcpClient.connect(InetSocketAddress(SERVER_IP_ADDRESS, 80))
    val outputStream = tcpClient.getOutputStream().buffered()
    outputStream.write(HTTP_HEADERS)

    val buffer = ByteArray(300000)

    while (true) {
        val packet = sendPackets.take()
        val size = packet.size

        putShort(buffer, 0, size.toShort())
        packet.copyInto(buffer, 2)

        outputStream.write(buffer, 0, packet.size + 2)
        outputStream.write(packet, 0, packet.size)
        outputStream.flush()
    }
}

//fun startReceivingConnection() {
//    runBlocking {
//        client.prepareGet("http://" + SERVER_IP_ADDRESS + "/receive").execute {
//            val inputStream = it.bodyAsChannel().toInputStream().buffered()
//            println("connected to receive")
//
//            val buffer = ByteArray(3000)
//            val sizeBuffer = ByteArray(2)
//            var readSum = 0
//
//            while (true) {
//                if (inputStream.read(sizeBuffer, 0, sizeBuffer.size) == -1) {
//                    println("is zero")
//                    exitProcess(0)
//                }
//                val size = byteArrayToShort(sizeBuffer)
//                readSum += size
//                println("size " + size)
//                println("read sum " + readSum)
////                if (size > 1380) {
////                    continue
////                }
//                if (inputStream.read(buffer, 0, size.toInt()) == -1) {
//                    println("is zero")
//                    exitProcess(0)
//                }
//                println("received ")
//                receivePackets.send(buffer.sliceArray(0 until size))
//            }
//        }
//    }
//}

fun startReceivingConnection() {
    val tcpClient = Socket()
    tcpClient.connect(InetSocketAddress(SERVER_IP_ADDRESS, 80))
    println("connected")
    val outputStream = tcpClient.getOutputStream().buffered()
    val inputStream = tcpClient.getInputStream().buffered()
    outputStream.write(HTTP_RECEIVE_HEADERS)
    outputStream.flush()
    println("write")

    while (true) {
        if (inputStream.read() == '\r'.toByte().toInt()) {
            if (inputStream.read() == '\n'.toByte().toInt()) {
                if (inputStream.read() == '\r'.toByte().toInt()) {
                    if (inputStream.read() == '\n'.toByte().toInt()) {
                        break
                    }
                }
            }
        }
    }

            val buffer = ByteArray(3000)
            val sizeBuffer = ByteArray(2)
            var readSum = 0

            while (true) {
                if (inputStream.read(sizeBuffer, 0, sizeBuffer.size) == -1) {
                    println("is zero")
                    exitProcess(0)
                }
                val size = byteArrayToShort(sizeBuffer)
                readSum += size
                println("size " + size)
                println("read sum " + readSum)
//                if (size > 1380) {
//                    continue
//                }
                if (inputStream.read(buffer, 0, size.toInt()) == -1) {
                    println("is zero")
                    exitProcess(0)
                }
                println("received ")
                receivePackets.put(buffer.sliceArray(0 until size))
            }
}

fun putShort(byteArray: ByteArray, index: Int, value: Short) {
    byteArray[index] = (value.toInt() shr 8).toByte()  // High byte
    byteArray[index + 1] = value.toByte()              // Low byte
}

fun byteArrayToShort(byteArray: ByteArray, index: Int = 0): Short {
    val high = byteArray[index].toInt() and 0xFF  // High byte (shifted to the left)
    val low = byteArray[index + 1].toInt() and 0xFF  // Low byte
    return ((high shl 8) or low).toShort()
}

fun main() {
    val packet = ByteArray(3)
    putShort(packet, 0, 383)
    println(byteArrayToShort(packet))
}
