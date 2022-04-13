package com.flyn.sarcopenia_server

import com.flyn.sarcopenia_server.server.Server
import java.util.*

private var isServerStart = true

fun main() {
    Thread {
        val cmdLine = Scanner(System.`in`)
        while (isServerStart) {
            val input = cmdLine.nextLine()
            input.split(" ").let {
                command(it[0], it.drop(1))
            }
        }
    }.start()
    Thread {
        Server.start()
    }.start()
}

private fun command(cmd: String, args: List<String>) {
    when (cmd) {
        "close" -> {
            isServerStart = false
            Server.stop()
        }
        "echo" -> {
            args.forEach { print("$it ") }
            println()
        }
        else -> {
            System.err.println("Not have this command!!")
        }
    }
}