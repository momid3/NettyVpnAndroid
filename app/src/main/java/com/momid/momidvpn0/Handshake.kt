package com.momid.momidvpn0

import com.momid.channel
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

var currentLocalIp: ByteArray? = null

class HandshakeHandler(val onConnect: () -> Unit, val onDisconnect: () -> Unit): SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.fireChannelActive()
        channel = ctx.channel()
        println("channel is active")
        val packet = ByteArray(8) {
            0
        }
        val data = Unpooled.wrappedBuffer(packet)
        ctx.writeAndFlush(data)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        val received = ByteArray(packet.readableBytes())
        packet.readBytes(received)
        println("handshake from server: ${received.size}")
        println("allocated ip: " + received.joinToString(".") {
            "" + (it.toInt() and 0xff)
        })
        currentLocalIp = received
        ctx.pipeline().remove(this)
        onConnect()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
