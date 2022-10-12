package com.flyn.sarcopenia_server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.flyn.sarcopenia_server.server.Server
import java.util.*

private var isServerStart = true

fun main() = application {
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
    Window(
        onCloseRequest = {
            Server.stop()
            exitApplication()
        },
        title = "Compose for Desktop",
        state = rememberWindowState(width = 300.dp, height = 300.dp)
    ) {
        val count = remember { mutableStateOf(0) }
        MaterialTheme {
            Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
                Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        count.value++
                    }) {
                    Text(if (count.value == 0) "Hello World" else "Clicked ${count.value}!")
                }
                Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        count.value = 0
                    }) {
                    Text("Reset")
                }
            }
        }
    }
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