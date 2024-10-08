package com.momid.momidvpn0

import com.momid.channel
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

var currentLocalIp: ByteArray? = null

val clientHandshake = "hello handshake from\n\n\n server allocated ip".toByteArray(charset("ASCII"))

val handShakes = listOf(
    "hello handshake from\n\n\n server allocated ip from handshakes".toByteArray(charset("ASCII")),
    "hello handshake from\n\n\n server allocated ip of some handshakes".toByteArray(charset("ASCII")),
    "hello handshake from\n\n\n server allocated ip handshake is received".toByteArray(charset("ASCII"))
)

class HandshakeHandler(val onConnect: () -> Unit, val onDisconnect: () -> Unit): SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.fireChannelActive()
        channel = ctx.channel()
        println("channel is active")
        val packet = handShakes[0]
        val data = Unpooled.wrappedBuffer(packet)
        ctx.writeAndFlush(data)
        clientHandshakeStep += 1
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        if (clientHandshakeStep < 3) {
            Thread.sleep(300)
            val packet = handShakes[clientHandshakeStep]
            val data = Unpooled.wrappedBuffer(packet)
            ctx.writeAndFlush(data)
            clientHandshakeStep += 1
        } else if (clientHandshakeStep == 3) {
            Thread.sleep(300)
            val packet = clientHandshake
            val data = Unpooled.wrappedBuffer(packet)
            ctx.writeAndFlush(data)
            clientHandshakeStep += 1
        } else {
//        if (packet.readableBytes() == 4) {
            val received = ByteArray(packet.readableBytes())
            packet.readBytes(received)
            println("handshake from server: ${received.size}")
            println("allocated ip: " + received.sliceArray(0 until 4).joinToString(".") {
                "" + (it.toInt() and 0xff)
            })
            currentLocalIp = received.sliceArray(0 until 4)
            ctx.pipeline().remove(this)
            onConnect()
//        }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
