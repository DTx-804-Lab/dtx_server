package com.flyn.sarcopenia_server.data

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class ResponseEncoder: MessageToByteEncoder<ResponseData>() {

    override fun encode(ctx: ChannelHandlerContext?, msg: ResponseData, out: ByteBuf) {
        out.writeInt(msg.value)
    }

}