package com.dtx804lab.dtx_server.server

import com.dtx804lab.dtx_netty_lib.base.RawMessageCodec
import com.dtx804lab.dtx_netty_lib.secure.AesCodec
import com.dtx804lab.dtx_netty_lib.secure.RsaCodec
import com.dtx804lab.dtx_netty_lib.secure.decodeHex
import com.dtx804lab.dtx_server.sql.SqlManager
import io.github.cdimascio.dotenv.dotenv
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import java.net.InetSocketAddress
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.sql.SQLException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Server {

    private val privateKey: RSAPrivateKey
    private val aesKeySpec: SecretKeySpec
    internal val ivSpec: IvParameterSpec

    init {
        val dotenv = dotenv {
            directory = "assets"
            filename = "env"
        }
        privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(dotenv["PRIVATE_KEY"]?.decodeHex())) as RSAPrivateKey
        aesKeySpec = SecretKeySpec(dotenv["SECRET_KEY"]?.decodeHex(), "AES")
        ivSpec = IvParameterSpec(dotenv["IV_PARAMETER"]?.decodeHex())
    }

    private var isServerStart = false
    private lateinit var bossGroup: NioEventLoopGroup
    private lateinit var workerGroup: NioEventLoopGroup

    fun start(port: Int = 8787) {
        if (isServerStart) return
        isServerStart = true
        try {
            SqlManager.start()
            SqlManager.init()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()
        try {
            println("Server start")
            val bind = ServerBootstrap().apply {
                group(bossGroup, workerGroup)
                channel(NioServerSocketChannel::class.java)
                childHandler(object: ChannelInitializer<NioSocketChannel>() {

                    override fun initChannel(ch: NioSocketChannel) {
                        with (ch.pipeline()) {
                            addLast(LengthFieldBasedFrameDecoder(10240, 0, 2, 0, 2))
                            addLast(LengthFieldPrepender(2))
                            addLast(AesCodec(aesKeySpec, ivSpec))
                            addLast(RawMessageCodec())
                            addLast(RsaCodec(privateKey = privateKey))
                            addLast(ConnectionHandler())
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
        println("Server close")
    }

    fun stop() {
        if (!isServerStart) return
        isServerStart = false
        try {
            SqlManager.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
    }

}