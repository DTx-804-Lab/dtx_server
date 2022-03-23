package com.flyn.sarcopenia_server.data

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder
import java.nio.charset.Charset

class RequestDecoder: ReplayingDecoder<RequestData>() {

    companion object {
        private val charset = Charset.forName("UTF-8")
    }

    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf, out: MutableList<Any>) {
        val value = `in`.readInt()
        val len = `in`.readInt()
        val str = `in`.readCharSequence(len, charset)
        out.add(RequestData(value, str.toString()))
    }

}