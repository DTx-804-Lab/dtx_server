package com.flyn.sarcopenia_server.handler

import com.flyn.sarcopenia_server.data.FileMessage
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class FileMsgHandler: SimpleChannelInboundHandler<FileMessage>() {

    companion object {
        private const val storagePath = "C:/Users/etern/Desktop/receiveData"
    }

    private var fileChannels: FileChannel? = null

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FileMessage) {
        if (!msg.isRemaining()) {
            try {
                val path = "$storagePath/${msg.uuid}/${msg.fileName}"
                File("$storagePath/${msg.uuid}").let { dir ->
                    if (!dir.exists()) dir.mkdirs()
                }
                File(path).createNewFile()
                val writer = RandomAccessFile(path, "rw")
                fileChannels = writer.channel
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
        }
        fileChannels?.let { channel ->
            channel.write(msg.data)
            if (!msg.remaining) {
                println("file receive finish")
                channel.close()
            }
        }?: run { println("not find channel") }
    }

}