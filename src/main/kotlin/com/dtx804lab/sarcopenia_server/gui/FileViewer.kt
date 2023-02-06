package com.dtx804lab.sarcopenia_server.gui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Preview
@Composable
fun previewFileViewer() {
    val r = Random()
    val list = mutableListOf<UserFile>()
    for (i in 0..1) {
        val uuid = UUID.randomUUID()
        for (j in 0..r.nextInt(3)) {
            list.add(UserFile(uuid, r.nextInt().toString()))
        }
    }
    FileViewer(list)
}


@Composable
fun FileViewer(files: MutableList<UserFile>) {
    if (files.isEmpty()) return
    val uuidList = files.map { it.uuid }.distinct()
    val state = rememberLazyListState()
    Box {
        LazyColumn(
            state = state,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 20.dp, bottom = 20.dp,start = 50.dp, end = 50.dp)
                .border(3.dp, Color.Black, shape = RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            uuidList.forEach { uuid ->
                val names = files.filter { it.uuid == uuid }.map { it.fileName }
                item {
                    Text(
                        uuid.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(
                    names,
                    key = { "${uuid}-$it" }
                ) {
                    Text(
                        it,
                        fontSize = 16.sp,
                        style = TextStyle(Color.Black),
                        modifier = Modifier.fillMaxWidth()
                            .background(Color.LightGray)
                            .padding(5.dp)
                    )
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
    }
}