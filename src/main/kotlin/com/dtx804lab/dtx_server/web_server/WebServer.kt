package com.dtx804lab.dtx_server.web_server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerExpectContinueHandler
import io.netty.handler.ssl.SslContextBuilder
import java.net.InetSocketAddress

object WebServer {

    private const val SSL = false
    private const val PORT = 48487

    private var isServerStart = false
    private lateinit var bossGroup: NioEventLoopGroup
    private lateinit var workerGroup: NioEventLoopGroup

    fun start() {
        if (isServerStart) return
        isServerStart = true
        bossGroup = NioEventLoopGroup()
        workerGroup = NioEventLoopGroup()
        try {
            println("Web server start")
            val bind = ServerBootstrap().apply {
                group(bossGroup, workerGroup)
                channel(NioServerSocketChannel::class.java)
                childHandler(object: ChannelInitializer<NioSocketChannel>() {

                    override fun initChannel(ch: NioSocketChannel) {
                        with (ch.pipeline()) {
//                            if (SSL) addLast(sslContext!!.newHandler(ch.alloc()))
                            addLast(HttpServerCodec())
                            addLast(HttpObjectAggregator(1024 * 1024))
                            addLast(HttpServerExpectContinueHandler())
                            addLast(HttpConnectionHandler())
                        }
                    }

                })
                option(ChannelOption.SO_BACKLOG, 128)
                childOption(ChannelOption.SO_KEEPALIVE, true)
            }.bind(InetSocketAddress(PORT)).sync()
            bind.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
        println("Web server close")
    }

    fun stop() {
        if (!isServerStart) return
        isServerStart = false
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
    }

}