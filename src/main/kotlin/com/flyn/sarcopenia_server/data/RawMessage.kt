package com.flyn.sarcopenia_server.data

import io.netty.buffer.ByteBuf

class RawMessage(val code: Byte, val message: ByteBuf)