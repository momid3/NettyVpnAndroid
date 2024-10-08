package com.momid

import com.momid.padding.unoffsetize
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.concurrent.ArrayBlockingQueue

val incomingInternetPackets = ArrayBlockingQueue<ByteArray>(10000)

class ClientHandler(val onDisconnect: () -> Unit) : SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
//        ctx.writeAndFlush("hello".toByteArray())
        channel = ctx.channel()
        println("channel is active")
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        val received = ByteArray(packet.readableBytes())
        packet.readBytes(received)
        println("received from server: ${received.size}")
        incomingInternetPackets.put(received.unoffsetize())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
//        onDisconnect()
    }
}
