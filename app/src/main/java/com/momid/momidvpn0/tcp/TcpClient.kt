package com.momid.momidvpn0.tcp

import com.momid.momidvpn0.HTTP_HEADERS
import com.momid.momidvpn0.HTTP_RECEIVE_HEADERS
import com.momid.momidvpn0.SERVER_IP_ADDRESS
import com.momid.momidvpn0.byteArrayToShort
import com.momid.momidvpn0.putShort
import com.momid.momidvpn0.receivePackets
import com.momid.momidvpn0.sendPackets
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.exitProcess

fun startSendConnection() {

    val tcpClient = Socket()
    tcpClient.connect(InetSocketAddress(SERVER_IP_ADDRESS, 33338))
    val outputStream = tcpClient.getOutputStream().buffered()

    val buffer = ByteArray(3000)

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

fun startReceivingConnection() {
    val tcpClient = Socket()
    tcpClient.connect(InetSocketAddress(SERVER_IP_ADDRESS, 33338))
    println("connected")
    val outputStream = tcpClient.getOutputStream().buffered()
    val inputStream = tcpClient.getInputStream().buffered()

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
