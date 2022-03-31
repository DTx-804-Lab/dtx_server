package com.flyn.sarcopenia_server.server

import com.flyn.sarcopenia_server.decoder.RawMessageDecoder
import com.flyn.sarcopenia_server.decoder.RequestDecoder
import com.flyn.sarcopenia_server.handler.ConnectionHandler
import com.flyn.sarcopenia_server.handler.FileMsgHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.InetSocketAddress

object Server {

    private var isServerStart = false
    private lateinit var bossGroup: NioEventLoopGroup
    private lateinit var workerGroup: NioEventLoopGroup

    fun start(port: Int = 8787) {
        if (isServerStart) return
        isServerStart = true
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()
        try {
            val bind = ServerBootstrap().apply {
                group(bossGroup, workerGroup)
                channel(NioServerSocketChannel::class.java)
                childHandler(object: ChannelInitializer<NioSocketChannel>() {

                    override fun initChannel(ch: NioSocketChannel) {
                        with (ch.pipeline()) {
                            addLast(RequestDecoder(), RawMessageDecoder(), ConnectionHandler())
                            addLast(FileMsgHandler())
                        }
                    }

                })
                option(ChannelOption.SO_BACKLOG, 128)
                childOption(ChannelOption.SO_KEEPALIVE, true)
            }.bind(InetSocketAddress(port)).sync()
            bind.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }

    }

    fun stop() {
        if (!isServerStart) return
        isServerStart = false
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
    }

}