package com.dtx804lab.dtx_server.web_server

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths


class HttpConnectionHandler : SimpleChannelInboundHandler<HttpObject>() {

    companion object {
        private const val FAVICON_ICO = "/favicon.ico"
        private val iconByte = getFileBytes("assets/favicon.ico")
        private val homePage = getFileBytes("assets/web/index.html")

        private fun getFileBytes(path: String): ByteBuf {
            File(path).let {
                return if (!it.exists()) Unpooled.EMPTY_BUFFER
                else Unpooled.copiedBuffer(Files.readAllBytes(Paths.get(it.path)))
            }
        }
    }

    private val responseData = StringBuilder()
    private lateinit var request: HttpRequest

    private fun formatParams(ctx: ChannelHandlerContext, request: HttpRequest) {
        QueryStringDecoder(request.uri()).run {
            println(path())

            if (path() == "/") {
                ctx.writeAndFlush(
                    makeResponse(Unpooled.copiedBuffer(homePage), "text/html; charset=UTF-8")
                )
                return
            }
            if (path() == FAVICON_ICO) {
                ctx.writeAndFlush(
                    makeResponse(Unpooled.copiedBuffer(iconByte), "image/x-icon")
                )
                return
            }
            if (path().endsWith(".js", true)) {
                ctx.writeAndFlush(
                    makeResponse(getFileBytes("assets/web${path()}"), "*/*")
                )
                return
            }
            if (path().endsWith(".css", true)) {
                ctx.writeAndFlush(
                    makeResponse(getFileBytes("assets/web${path()}"), "text/css")
                )
                return
            }
            if (parameters().isEmpty()) return
            responseData.append("Parameter: \n")
            parameters().forEach { (key, values) ->
                responseData.append("  $key = ${values.joinToString(separator = ", ", postfix = "\n")}")
            }
        }
    }

    private fun formatBody(httpContent: HttpContent) {
        val content = httpContent.content()
        if (!content.isReadable) return
        responseData.append(content.toString(Charset.forName("UTF-8")))
        responseData.append("\n")
    }

    private fun dealContent(request: FullHttpRequest) {
        request.headers().get("Content-Type")?.toString()?.let {
            when (it.split(";")[0]) {
                "application/json" -> {}
                "application/x-www-form-urlencoded" -> {
                    formatBody(request)
                }
            }
        }
    }

    private fun prepareLastResponse(trailer: LastHttpContent) {
        if (trailer.trailingHeaders().isEmpty) return
        trailer.trailingHeaders().names().forEach { name ->
            trailer.trailingHeaders().getAll(name).forEach { value ->
                responseData.append("$name = $value\n")
            }
        }
    }

    private fun makeResponse(content: ByteBuf, contentType: String): DefaultFullHttpResponse {
        val response = DefaultFullHttpResponse(
            request.protocolVersion(),
            if (request.decoderResult().isSuccess) HttpResponseStatus.OK
            else HttpResponseStatus.BAD_REQUEST,
            content
        )
        response.headers().apply {
            set(HttpHeaderNames.CONTENT_TYPE, contentType)
            setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
            if (isKeepAlive()) {
                if (request.protocolVersion().isKeepAliveDefault) {
                    set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                }
            }
            else set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        }
        return response
    }

    private fun isKeepAlive() = HttpUtil.isKeepAlive(request)

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
        if (msg is HttpRequest) {
            request = msg
            // if the request expects a 100 Continue status. In that case, we immediately write back with an empty response with a status of CONTINUE
            if (HttpUtil.is100ContinueExpected(msg)) {
                ctx.write(DefaultFullHttpResponse(
                    msg.protocolVersion(), HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER
                ))
            }
            responseData.setLength(0)

            when (msg.method()) {
                HttpMethod.GET -> {
                    formatParams(ctx, msg)
                }
                HttpMethod.POST -> {
                    dealContent(msg as FullHttpRequest)
                }
            }

        }

        if (msg is LastHttpContent) {
            prepareLastResponse(msg)
            ctx.writeAndFlush(makeResponse(
                Unpooled.copiedBuffer(responseData.toString(), Charset.forName("UTF-8")),
                "text/plain; charset=UTF-8"
            )).run {
                if (!isKeepAlive()) {
                    println("context close")
                    addListener(ChannelFutureListener.CLOSE)
                }
            }
        }
    }

}