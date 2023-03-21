package com.dtx804lab.dtx_server.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dtx804lab.dtx_server.gui.UserFile

object FileManager {

//    internal var FILE_ROOT by mutableStateOf("${System.getProperty("user.home")}\\Desktop\\Server")
    internal var FILE_ROOT by mutableStateOf("Server")
    internal val STORAGE_PATH
        get() = "$FILE_ROOT/receiveData"
    internal val FILE_LIST = mutableStateListOf<UserFile>()
}