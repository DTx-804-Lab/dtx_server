package com.dtx804lab.dtx_server.web_server

import com.dtx804lab.dtx_server.utils.FileManager
import com.dtx804lab.dtx_server.utils.JsonUtil
import com.dtx804lab.dtx_server.sql.SqlManager
import com.fasterxml.jackson.databind.JsonNode
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import io.netty.handler.codec.http.multipart.FileUpload
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import io.netty.util.CharsetUtil
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.properties.Delegates


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

    private var decoder: HttpPostRequestDecoder? = null
    private var startTime by Delegates.notNull<Long>()
    private lateinit var request: HttpRequest

    private fun writeResponse(ctx: ChannelHandlerContext, content: ByteBuf, contentType: String) {
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
        ctx.writeAndFlush(response)
    }

    private fun closeResponse(ctx: ChannelHandlerContext) {
        val response = DefaultFullHttpResponse(
            request.protocolVersion(),
            if (request.decoderResult().isSuccess) HttpResponseStatus.OK
            else HttpResponseStatus.BAD_REQUEST,
            Unpooled.copiedBuffer("File uploaded", CharsetUtil.UTF_8)
        )
        response.headers().apply {
            set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8")
            setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
            set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        }
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

    private fun processGetMethod(ctx: ChannelHandlerContext, path: String, request: HttpRequest) {
        if (path == "/") {
            writeResponse(ctx, Unpooled.copiedBuffer(homePage), "text/html; charset=UTF-8")
            return
        }
        if (path == FAVICON_ICO) {
            writeResponse(ctx, Unpooled.copiedBuffer(iconByte), "image/x-icon")
            return
        }
        if (path.startsWith("/users")) {
            val users = SqlManager.getUserList().toByteArray()
            writeResponse(ctx, Unpooled.copiedBuffer(users), "application/json")
            return
        }
        if (path.endsWith(".js", true)) {
            writeResponse(ctx, getFileBytes("assets/web${path}"), "*/*")
            return
        }
        if (path.endsWith(".css", true)) {
            writeResponse(ctx, getFileBytes("assets/web${path}"), "text/css")
            return
        }
        if (path.endsWith(".txt", true)) {
            writeResponse(ctx, getFileBytes("assets/web${path}"), "text/plain; charset=UTF-8")
            return
        }
    }

    private fun processPostMethod(ctx: ChannelHandlerContext, path: String, request: FullHttpRequest) {
        if (path.startsWith("/upload")) {
            decoder = HttpPostRequestDecoder(DefaultHttpDataFactory(true), request)
            return
        }
        val node = formatJsonContent(request.content())?: return
        if (path.startsWith("/files")) {
            val files = SqlManager.getFileList(node.get("uuid").asText()).toByteArray()
            writeResponse(ctx, Unpooled.copiedBuffer(files), "application/json")
            return
        }
        if (path.startsWith("/download")) {
            val uuid = node.get("uuid").asText()
            val fileName = node.get("fileName").asText()
            writeResponse(ctx, getFileBytes("${FileManager.STORAGE_PATH}/$uuid/$fileName"), "*/*")
            return
        }
    }

    private fun formatJsonContent(data: ByteBuf): JsonNode? {
        if (!data.isReadable) return null
        return JsonUtil.mapper.readTree(data.toString(Charset.forName("UTF-8")))
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
            val path = QueryStringDecoder(request.uri()).path()
            println(path)
            when (msg.method()) {
                HttpMethod.GET -> processGetMethod(ctx, path, msg)
                HttpMethod.POST -> processPostMethod(ctx, path, msg as FullHttpRequest)
            }

        }
        if (decoder != null && msg is HttpContent) {
            decoder!!.offer(msg)
            if (msg is LastHttpContent) {
                val file = decoder!!.getBodyHttpData("file")
                if (file is FileUpload) {
                    println(file.filename)
                }
                println(decoder!!.getBodyHttpData("uuid"))
                println(decoder!!.getBodyHttpData("key"))
                decoder!!.destroy()
                decoder = null
//                writeResponse(ctx, Unpooled.copiedBuffer("File uploaded", CharsetUtil.UTF_8), "text/html; charset=UTF-8")
//                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                closeResponse(ctx)
            }
        }

    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        println("ConnectionHandler")
        startTime = System.currentTimeMillis()
        println(ctx.channel().remoteAddress())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        println(System.currentTimeMillis() - startTime)
    }

}