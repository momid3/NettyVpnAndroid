package com.momid

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.concurrent.ArrayBlockingQueue

val incomingInternetPackets = ArrayBlockingQueue<ByteArray>(300)

class ClientHandler(val onDisconnect: () -> Unit) : SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
//        ctx.writeAndFlush("hello".toByteArray())
        channel = ctx
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        val received = ByteArray(packet.readableBytes())
        packet.readBytes(received)
        incomingInternetPackets.put(received)
//        println("received from server: ${received.size}")
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
        onDisconnect()
    }
}
