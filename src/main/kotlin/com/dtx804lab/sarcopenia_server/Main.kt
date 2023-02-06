package com.dtx804lab.sarcopenia_server

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
import com.dtx804lab.sarcopenia_server.gui.FileViewer
import com.dtx804lab.sarcopenia_server.gui.UserFile
import com.dtx804lab.sarcopenia_server.server.Server
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

fun main() = application {
    Window(
        onCloseRequest = {
            Server.stop()
            exitApplication()
        },
        icon = painterResource("dtx_icon.jpg"),
        title = "Sarcopenia Server",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        app()
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
@OptIn(DelicateCoroutinesApi::class)
fun TopBar() {
    var isServerStart by remember { mutableStateOf(false) }
    Surface(color = Color.LightGray) {
         Row(horizontalArrangement = Arrangement.SpaceBetween) {
             Button(
                 onClick = {
                     if (isServerStart) Server.stop()
                     else GlobalScope.launch(Dispatchers.Default) {
                         Server.start()
                     }
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