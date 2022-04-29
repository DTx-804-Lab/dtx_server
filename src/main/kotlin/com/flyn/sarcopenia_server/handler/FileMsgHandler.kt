package com.flyn.sarcopenia_server.handler

import com.flyn.fc_message.message.FileMessage
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

class FileMsgHandler: SimpleChannelInboundHandler<FileMessage>() {

    companion object {
        private const val storagePath = "C:/Users/etern/Desktop/receiveData"
//        private const val storagePath = "./receiveData"
    }

    private var fileChannels: FileChannel? = null

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FileMessage) {
        if (fileChannels == null) {
            try {
                val path = "$storagePath/${msg.uuid}/${msg.fileName.replace(":", "_")}"
                File("$storagePath/${msg.uuid}").let { dir ->
                    if (!dir.exists()) dir.mkdirs()
                }
                File(path).createNewFile()
                val writer = RandomAccessFile(path, "rw")
                fileChannels = writer.channel
            } catch (exception: IOException) {
                exception.printStackTrace()
                return
            }
        }
        fileChannels?.let { channel ->
            channel.write(msg.data)
            if (!msg.remaining) {
                println("file receive finish")
                channel.close()
                fileChannels = null
            }
        }?: run { println("not find channel") }
    }

}