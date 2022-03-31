package com.flyn.sarcopenia_server.decoder

import com.flyn.sarcopenia_server.data.RawMessage
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder
import kotlin.properties.Delegates

class RequestDecoder: ReplayingDecoder<DecoderState>(DecoderState.MESSAGE_TYPE) {

    private var code by Delegates.notNull<Byte>()
    private var length by Delegates.notNull<Int>()

    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) {
        println("decoding")
        when (state()) {
            DecoderState.MESSAGE_TYPE -> {
                code = `in`.readByte()
                if (!MessageType.types.containsValue(code)) {
                    ctx.channel().close()
                    return
                }
                checkpoint(DecoderState.MESSAGE_LENGTH)
            }
            DecoderState.MESSAGE_LENGTH -> {
                length = `in`.readInt()
                checkpoint(DecoderState.MESSAGE)
            }
            DecoderState.MESSAGE -> {
                val msg = `in`.readBytes(length)
                out.add(RawMessage(code, msg))
                println("out msg $code")
                checkpoint(DecoderState.MESSAGE_TYPE)
            }
            else -> throw Error("Shouldn't reach here.")
        }
    }

}