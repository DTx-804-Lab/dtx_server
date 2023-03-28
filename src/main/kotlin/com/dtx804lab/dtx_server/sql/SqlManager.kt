package com.dtx804lab.dtx_server.sql

import com.dtx804lab.dtx_server.utils.JsonUtil
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object SqlManager {

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneId.systemDefault())

    private lateinit var pdDatabase: Connection

    fun init() {
        pdDatabase.createStatement().let { statement ->
            statement.queryTimeout = 30
            StringBuilder().run {
                append("create table if not exists user (")
                append("patientID int primary key,")
                append("name ntext not null,")
                append("uuid char(36) unique not null")
                append(")")
                statement.execute(this.toString())
            }
            StringBuilder().run {
                append("create table if not exists files (")
                append("fileName text(256) not null,")
                append("uuid char(36) not null,")
                append("date int8 not null,")
                append("type text(16) not null,")
                append("foreign key (uuid) references user(uuid)")
                append(")")
                statement.execute(this.toString())
            }
        }
    }

    fun registerPD(id: Int, name: String): UUID {
        val statement = pdDatabase.createStatement()
        statement.queryTimeout = 30
        println("$id: $name")
        val result = statement.executeQuery("select patientID from user where patientId = $id")
        if (result.next()) return UUID(0, 0)
        val uuid = UUID.randomUUID()
        statement.executeUpdate("insert into user (patientID, name, uuid) values ($id, '$name', '$uuid')")
        return uuid
    }

    fun loginPD(id: Int, name: String): UUID {
        val statement = pdDatabase.createStatement()
        statement.queryTimeout = 30
        println("$id: $name")
        val result = statement.executeQuery("select name, uuid from user where patientId = $id")
        return if (result.next() && result.getString("name") == name) UUID.fromString(result.getString("uuid"))
        else UUID(0, 0)
    }

    fun saveFile(uuid: UUID, fileName: String) {
        val (type, date) = fileNameSplitter(fileName)
        val statement = pdDatabase.createStatement()
        statement.queryTimeout = 30
        statement.executeUpdate("insert into files (fileName, uuid, date, type) values ('$fileName', '$uuid', $date, '$type')")
    }

    fun getFileList(uuid: String): String {
        val statement = pdDatabase.createStatement()
        statement.queryTimeout = 30
        val response = JsonUtil.mapper.createObjectNode()
        StringBuilder().run {
            append("select fileName from files where uuid = '$uuid'")
            statement.executeQuery(this.toString())
        }.run {
            val fileList = response.putArray("files")
            while (next()) {
                fileList.add(getString("fileName"))
            }
        }
        return JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
    }

    fun getUserList(): String {
        val response = JsonUtil.mapper.createObjectNode()
        pdDatabase.createStatement().run {
            queryTimeout = 30
            executeQuery("select patientID, name, uuid from user")
        }.run {
            val userList = response.putArray("user")
            while (next()) {
                val user = JsonUtil.mapper.createObjectNode()
                user.put("id", getString("patientID"))
                user.put("name", getString("name"))
                user.put("uuid", getString("uuid"))
                userList.add(user)
            }
        }
        return  JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
    }

    fun start() {
        pdDatabase = DriverManager.getConnection("jdbc:sqlite:pd.db")
    }

    fun close() {
        pdDatabase.close()
    }

    private fun fileNameSplitter(name: String) : Pair<String, String> {
        name.split("_").let {
            val instant = Instant.ofEpochMilli(it[1].toLong())
            return it[0] to formatter.format(instant)
        }
    }

}