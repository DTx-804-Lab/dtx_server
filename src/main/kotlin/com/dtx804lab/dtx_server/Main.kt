package com.dtx804lab.dtx_server

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dtx804lab.dtx_server.gui.FileViewer
import com.dtx804lab.dtx_server.gui.UserFile
import com.dtx804lab.dtx_server.server.Server
import com.dtx804lab.dtx_server.utils.FileManager
import com.dtx804lab.dtx_server.web_server.WebServer
import java.io.File
import java.util.*

private var isRunning = mutableStateOf(true)
private var isServerStart = mutableStateOf(false)
private var isVisible = mutableStateOf(true)

fun main() = run {
    Thread {
        println("create thread")
        Scanner(System.`in`).use { input ->
            while (isRunning.value) {
                input.nextLine().split(" ").let {
                    when (it[0]) {
                        "start" -> {
                            serverStart()
                            isServerStart.value = true
                        }
                        "stop" -> {
                            serverStop()
                            isServerStart.value = false
                        }
                        "open" -> isVisible.value = true
                        "close" -> isVisible.value = false
                        "exit" -> {
                            isVisible.value = true
                            isRunning.value = false
                        }
                    }
                    println(it[0])
                }
            }
        }
        println("thread finish")
    }.start()
    application {
        val isVisible by remember { isVisible }
        val isRunning by remember { isRunning }
        Window(
            onCloseRequest = {
                serverStop()
                exitApplication()
            },
            visible = isVisible,
            icon = painterResource("dtx_icon.jpg"),
            title = "Sarcopenia Server",
            state = rememberWindowState(width = 800.dp, height = 600.dp)
        ) {
            app()
            if (!isRunning) {
                serverStop()
                exitApplication()
            }
        }
    }
}

@Preview
@Composable
fun app() {
    val fileList = remember { FileManager.FILE_LIST }
    MaterialTheme {
        Scaffold(
            topBar = { TopBar() }
        ) {
            val dir = File(FileManager.STORAGE_PATH)
            FileManager.FILE_LIST.clear()
            if (dir.exists()) {
                dir.listFiles()?.forEach {
                    if (it.isDirectory) {
                        it.listFiles()?.forEach { file ->
                            try {
                                fileList.add(UserFile(UUID.fromString(it.name), file.name))
                            } catch (e: IllegalArgumentException) {
                                println("Directory is not match uuid")
                            }
                        }
                    }
                }
            }
            FileViewer(fileList)
        }
    }
}

@Preview
@Composable
fun TopBar() {
    var isServerStart by remember { isServerStart }
    Surface(color = Color.LightGray) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = {
                    if (isServerStart) serverStop()
                    else serverStart()
                    isServerStart = !isServerStart
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 8.dp, end = 8.dp)
            ) {
                Text(if (isServerStart) "Server stop" else "Server start")
            }
            TextField(
                value = File(FileManager.FILE_ROOT).absolutePath,
                onValueChange = {
                    FileManager.FILE_ROOT = Regex("[^a-zA-Z0-9\\\\:_]").replace(it, "")
                },
                label = { Text("Server Path") },
                modifier = Modifier.align(Alignment.CenterVertically)
                    .fillMaxWidth()
            )
        }
    }
}

private fun serverStart() {
    Thread { Server.start() }.start()
    Thread { WebServer.start() }.start()
}

private fun serverStop() {
    Server.stop()
    WebServer.stop()
}