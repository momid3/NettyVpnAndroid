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

fun startClient(onConnect: () -> Unit, onDisconnect: () -> Unit): Boolean {
    val group: EventLoopGroup = NioEventLoopGroup()
//    try {
        // Create SSL context
        val sslContext: SslContext = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE) // For self-signed certificates (Not recommended for production)
            .build()

        val bootstrap = Bootstrap()
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

        // Connect to the server
    val channelFuture: ChannelFuture
    try {
        channelFuture = bootstrap.connect("194.146.123.180", 443).sync()
    } catch (t: Throwable) {
//        t.printStackTrace()
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
//        onDisconnect()
    }
    return true
}
