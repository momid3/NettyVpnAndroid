package com.momid.momidvpn0.tcp

import com.momid.momidvpn0.HTTP_HEADERS
import com.momid.momidvpn0.HTTP_RECEIVE_HEADERS
import com.momid.momidvpn0.SERVER_IP_ADDRESS
import com.momid.momidvpn0.byteArrayToShort
import com.momid.momidvpn0.putShort
import com.momid.momidvpn0.receivePackets
import com.momid.momidvpn0.sendPackets
import com.momid.momidvpn0.tcpOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.experimental.xor
import kotlin.system.exitProcess

var tcpSocket: Socket? = null

fun startSendConnection() {

    val tcpClient = Socket()
    tcpClient.connect(InetSocketAddress(SERVER_IP_ADDRESS, 33338))
    val outputStream = tcpClient.getOutputStream()
    tcpSocket = tcpClient

    val buffer = ByteArray(30000)

    while (true) {
        val packet = sendPackets.take()
        val size = packet.size

//        putShort(buffer, 0, size.toShort())
//        packet.copyInto(buffer, 2)

//        outputStream.write(buffer, 0, packet.size + 2)
        outputStream.write(packet, 0, packet.size)
        outputStream.flush()
    }
}

fun startReceivingConnection() {
    val tcpClient = Socket()
    tcpClient.connect(InetSocketAddress(SERVER_IP_ADDRESS, 33338))
    println("connected")
    val outputStream = tcpClient.getOutputStream()
    val inputStream = tcpClient.getInputStream()

    val buffer = ByteArray(30000)
    val sizeBuffer = ByteArray(2)
    var readSum = 0

    while (true) {
//        if (inputStream.read(sizeBuffer, 0, sizeBuffer.size) == -1) {
//            println("is zero")
//            exitProcess(0)
//        }
//        val size = byteArrayToShort(sizeBuffer)
//        readSum += size
//        println("size " + size)
//        println("read sum " + readSum)
//                if (size > 1380) {
//                    continue
//                }
        val size = inputStream.read(buffer, 0, buffer.size)
        if (size == -1) {
            println("is zero")
            exitProcess(0)
        }
        println("received ")
        receivePackets.put(buffer.sliceArray(0 until size))
    }
}

fun startSendingAndReceiving(onConnect: () -> Unit, onReceive: (ByteArray) -> Unit): OutputStream {
    val tcpClient = Socket()
    tcpClient.tcpNoDelay = true
//    tcpClient.receiveBufferSize = 30000
//    tcpClient.sendBufferSize = 1800
    tcpClient.keepAlive = true
    tcpClient.connect(InetSocketAddress(SERVER_IP_ADDRESS, 3333))
    tcpSocket = tcpClient
    onConnect()
    val inputStream = tcpClient.getInputStream()
    val outputStream = tcpClient.getOutputStream()

    tcpOutputStream = outputStream

//    Thread {
//        val buffer = ByteArray(3000)
//
//        while (true) {
//            val packet = sendPackets.take()
//            val size = packet.size
//
////        putShort(buffer, 0, size.toShort())
////        packet.copyInto(buffer, 2)
//
////        outputStream.write(buffer, 0, packet.size + 2)
//            outputStream.write(packet, 0, packet.size)
//            outputStream.flush()
//        }
//    }.start()

//    Thread {
        val buffer = ByteArray(30000)
        val sizeBuffer = ByteArray(2)
        var readSum = 0

        while (true) {
            if (!inputStream.readExactly(sizeBuffer, 0, sizeBuffer.size)) {
                println("is zero")
                exitProcess(0)
            }

            val size = byteArrayToShort(sizeBuffer)
//        readSum += size
//        println("size " + size)
//        println("read sum " + readSum)
//                if (size > 1380) {
//                    continue
//                }

            if (!inputStream.readExactly(buffer, 0, size.toInt())) {
                println("is zero")
                exitProcess(0)
            }
            println("received " + size)
            val packet = buffer.copyOfRange(0, size.toInt())
            xorDecode(packet)
            onReceive(packet)
        }
//    }.start()
}

fun InputStream.readExactly(buffer: ByteArray, offset: Int, length: Int): Boolean {
    var currentOffset = offset
    var sizeLeft = length
    while (true) {
        val size = this.read(buffer, currentOffset, sizeLeft)
        if (size == -1) {
            return false
        }

        sizeLeft -= size
        currentOffset += size

        if (sizeLeft == 0) {
            return true
        }
    }
}

fun OutputStream.writeWithSize(packet: ByteArray, buffer: ByteArray) {
    putShort(buffer, 0, packet.size.toShort())
    packet.copyInto(buffer, 2)
    this.write(buffer, 0, packet.size + 2)
    this.flush()
}

fun xorEncode(data: ByteArray) {
    for (i in data.indices) {
        data[i] = data[i].xor(38)
    }
}

fun xorDecode(data: ByteArray) {
    for (i in data.indices) {
        data[i] = data[i].xor(38)
    }
}
