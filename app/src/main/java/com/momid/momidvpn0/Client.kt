package com.momid

import com.momid.momidvpn0.HandshakeHandler
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

var channel: Channel? = null

val bootstrap = Bootstrap()

var vpnOnDisconnect: (() -> Unit)? = null

fun initClient(onConnect: () -> Unit, onDisconnect: () -> Unit) {
    val group: EventLoopGroup = NioEventLoopGroup()
//    try {
    // Create SSL context
    val sslContext: SslContext = SslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE) // For self-signed certificates (Not recommended for production)
        .build()

    vpnOnDisconnect = onDisconnect

    bootstrap.group(group)
        .channel(NioSocketChannel::class.java)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(object : ChannelInitializer<SocketChannel>() {
            @Throws(Exception::class)
            override fun initChannel(ch: SocketChannel) {
                val pipeline = ch.pipeline()
                // Add SSL handler first to encrypt and decrypt everything
                pipeline.addLast(sslContext.newHandler(ch.alloc()))
                // Add frame decoder and prepender
                pipeline.addLast(LengthFieldBasedFrameDecoder(3800, 0, 4, 0, 4))
                pipeline.addLast(LengthFieldPrepender(4))
                // Add the main handler
                pipeline.addLast(HandshakeHandler(onConnect, onDisconnect))
                pipeline.addLast(ClientHandler(onDisconnect))
            }
        })
}

fun startClient(): Boolean {
        // Connect to the server
    val channelFuture: ChannelFuture
    try {
        channelFuture = bootstrap.connect("194.146.123.180", 443).sync()
    } catch (t: Throwable) {
//        t.printStackTrace()
        println("did not connect")
        val onDisconnect = vpnOnDisconnect ?: throw (Throwable("client not initialized"))
        onDisconnect()
        return false
    }
        println("client connected to server")
//    channelFuture.channel().writeAndFlush("hello".toByteArray())

        // Wait until the connection is closed
//        channelFuture.channel().closeFuture().sync()
//    } finally {
//        group.shutdownGracefully()
//    }
    channelFuture.channel().closeFuture().addListener {
        println("disconnected")
        val onDisconnect = vpnOnDisconnect ?: throw (Throwable("client not initialized"))
        onDisconnect()
    }
    return true
}
