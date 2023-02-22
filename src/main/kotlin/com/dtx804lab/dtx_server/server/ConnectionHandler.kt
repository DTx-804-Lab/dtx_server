package com.dtx804lab.dtx_server.server

import com.dtx804lab.dtx_netty_lib.message.*
import com.dtx804lab.dtx_server.FileManager
import com.dtx804lab.dtx_server.gui.UserFile
import com.dtx804lab.dtx_server.sql.SqlManager
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.Key
import java.util.*
import javax.crypto.Cipher
import kotlin.properties.Delegates

class ConnectionHandler: ChannelInboundHandlerAdapter() {

    companion object {
        private val cipher = Cipher.getInstance("AES/CTR/NoPadding")
    }

    private val buffer = ByteBuffer.allocateDirect(1024 * 32)

    private var startTime by Delegates.notNull<Long>()
    private var fileChannels: FileChannel? = null

    private lateinit var uuid: UUID
    private lateinit var key: Key

    private fun fileReceive(msg: FileMessage) {
        if (fileChannels == null) {
            try {
                val path = "${FileManager.STORAGE_PATH}/$uuid/${msg.fileName.replace(":", "_")}"
                val dir = File("${FileManager.STORAGE_PATH}/$uuid")
                if (!dir.exists()) dir.mkdirs()
                File(path).createNewFile()
                fileChannels = RandomAccessFile(path, "rw").channel
                cipher.init(Cipher.DECRYPT_MODE, key, Server.ivSpec)
            } catch (exception: IOException) {
                exception.printStackTrace()
                return
            }
        }
        fileChannels?.let { channel ->
            if (msg.remaining) cipher.update(msg.data, buffer)
            else cipher.doFinal(msg.data, buffer)
            buffer.flip()
            channel.write(buffer)
            buffer.clear()

            channel.write(msg.data)

            if (!msg.remaining) {
                println("file receive finish")
                channel.close()
                fileChannels = null
                UserFile(uuid, msg.fileName).takeIf {
                    !FileManager.FILE_LIST.contains(it)
                }?.let {
                    FileManager.FILE_LIST.add(it)
                }
            }
        }?: run { println("not find channel") }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        println("ConnectionHandler")
        startTime = System.currentTimeMillis()
        println(ctx.channel().remoteAddress())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        println(System.currentTimeMillis() - startTime)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        when (msg::class) {
            UUIDMessage::class -> uuid = (msg as UUIDMessage).uuid
            KeyMessage::class -> key = (msg as KeyMessage).key
            FileMessage::class -> fileReceive(msg as FileMessage)
            LoginMessagePD::class -> {
                msg as LoginMessagePD
                val uuid = SqlManager.loginPD(msg.patientID, msg.patientName)
                ctx.writeAndFlush(UUIDMessage.encoder(ctx, UUIDMessage(uuid))).let {
                    it.addListener { ctx.close() }
                }
            }
            RegisterMessagePD::class -> {
                msg as RegisterMessagePD
                val uuid = SqlManager.registerPD(msg.patientID, msg.patientName)
                ctx.writeAndFlush(UUIDMessage.encoder(ctx, UUIDMessage(uuid))).let {
                    it.addListener { ctx.close() }
                }
            }
        }
    }

}