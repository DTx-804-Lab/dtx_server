package com.flyn.sarcopenia_server.decoder

object MessageType {

    var types = mapOf<String, Byte>(
        "FILE_TRANSFER" to 1,
        "REMAINING_FILE" to 2
    )
        private set

}