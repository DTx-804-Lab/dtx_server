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
        private val iconByte = getFileBytes("assets/web/favicon.ico")
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

            val (buffer, type) = formatUrl(path())
            ctx.writeAndFlush(makeResponse(buffer, type))

            if (parameters().isEmpty()) return
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

    private fun formatUrl(path: String): Pair<ByteBuf, String> {
        if (path == "/")
            return Unpooled.copiedBuffer(homePage) to "text/html; charset=UTF-8"
        if (path == FAVICON_ICO)
            return Unpooled.copiedBuffer(iconByte) to "image/x-icon"
        if (path.endsWith(".js", true))
            return getFileBytes("assets/web${path}") to "*/*"
        if (path.endsWith(".css", true))
            return getFileBytes("assets/web${path}") to "text/css"
        if (path.endsWith(".txt", true))
            return getFileBytes("assets/web${path}") to "text/plain; charset=UTF-8"
        return Unpooled.EMPTY_BUFFER to "*/*"
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