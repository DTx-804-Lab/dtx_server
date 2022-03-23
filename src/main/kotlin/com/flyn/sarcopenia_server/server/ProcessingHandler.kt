package com.flyn.sarcopenia_server.server

import com.flyn.sarcopenia_server.data.RequestData
import com.flyn.sarcopenia_server.data.ResponseData
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class ProcessingHandler: ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val requestData = msg as RequestData
        val responseData = ResponseData(requestData.value * 2)
        ctx.writeAndFlush(responseData).addListener(ChannelFutureListener.CLOSE)
        println(requestData)
        if (msg.message == "close") {
            Server.stop()
        }
    }

}