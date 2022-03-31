package com.flyn.sarcopenia_server.decoder

import com.flyn.sarcopenia_server.data.FileMessage
import com.flyn.sarcopenia_server.data.RawMessage
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import io.netty.util.ReferenceCountUtil
import java.lang.ref.Reference
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

class RawMessageDecoder: MessageToMessageDecoder<RawMessage>() {

    companion object {
        private val charset = Charset.forName("UTF-8")
    }

    override fun decode(ctx: ChannelHandlerContext, msg: RawMessage, out: MutableList<Any>) {
        when (msg.code) {
            // FILE_TRANSFER
            1.toByte() -> {
                // mostUUID: Long | leastUUID: Long | nameLength: Int | name: String |
                // remaining: Boolean | dataLength: Int | data: ByteArray
                with (msg.message) {
                    val uuid = UUID(readLong(), readLong())
                    val fileName = readCharSequence(readInt(), charset)
                    val remaining = readBoolean()
                    val data = ByteBuffer.allocateDirect(readInt())
                    getBytes(readerIndex(), data)
                    data.flip()
                    println("remaining $remaining")
                    FileMessage(uuid, fileName.toString(), remaining, data).run {
                        if (isRemaining()) return
                        out.add(this)
                    }
                }
            }
            // REMAINING_FILE
            2.toByte() -> {
                // remaining: Boolean | dataLength: Int | data: ByteArray
                with (msg.message) {
                    val uuid = FileMessage.REMAINING_FILE_UUID
                    val fileName = FileMessage.REMAINING_FILE_NAME
                    val remaining = readBoolean()
                    val data = ByteBuffer.allocateDirect(readInt())
                    getBytes(readerIndex(), data)
                    data.flip()
                    println("remaining $remaining")
                    out.add(FileMessage(uuid, fileName, remaining, data))
                }
            }
        }
        ReferenceCountUtil.release(msg.message)
    }

}