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
    private lateinit var fileDatabase: Connection

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
        }
        fileDatabase.createStatement().let { statement ->
            statement.queryTimeout = 30
            StringBuilder().run {
                append("create table if not exists files (")
                append("fileName text(256) not null,")
                append("uuid char(36) not null,")
                append("date int8 not null,")
                append("type text(16) not null")
                append(")")
                statement.execute(this.toString())
            }
        }
        updateToken()
    }

    fun registerPD(id: Int, name: String): UUID {
        val statement = pdDatabase.createStatement()
        statement.queryTimeout = 30
        println("$id: $name")
        val result = statement.executeQuery("select patientID from user where patientId = $id")
        if (result.next()) return UUID(0, 0)
        val uuid = UUID.randomUUID()
        statement.executeUpdate("insert into user (patientID, name, uuid) values ($id, '$name', '$uuid')")
        statement.executeUpdate("insert into tokens (uuid, token) values ('$uuid', random())")
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
        val statement = fileDatabase.createStatement()
        statement.queryTimeout = 30
        statement.executeUpdate("insert into files (fileName, uuid, date, type) values ('$fileName', '$uuid', $date, '$type')")
    }

    fun getUuidText(token: Long): String? {
        pdDatabase.createStatement().run {
            queryTimeout = 30
            executeQuery("select uuid from tokens where token = $token").let {
                while (it.next()) return it.getString("uuid")
            }
            return null
        }
    }

    fun getFileList(token: Long): String {
        val uuid = getUuidText(token)?: return "{}"
        val statement = fileDatabase.createStatement()
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
            executeQuery("select name, token from tokens")
        }.run {
            val userList = response.putArray("user")
            while (next()) {
                val user = JsonUtil.mapper.createObjectNode()
                user.put("name", getString("name"))
                user.put("token", getString("token"))
                userList.add(user)
            }
        }
        return  JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
    }

    fun start() {
        pdDatabase = DriverManager.getConnection("jdbc:sqlite:pd.db")
        fileDatabase = DriverManager.getConnection("jdbc:sqlite:file.db")
    }

    fun close() {
        pdDatabase.close()
        fileDatabase.close()
    }

    private fun updateToken() {
        pdDatabase.createStatement().run {
            queryTimeout = 30
            execute("drop table if exists tokens")
            StringBuilder().let {
                it.append("create table tokens (")
                it.append("uuid char(36) primary key,")
                it.append("name ntext,")
                it.append("token int unique not null,")
                it.append("foreign key (uuid) references user(uuid),")
                it.append("foreign key (name) references user(name)")
                it.append(")")
                execute(it.toString())
            }
            executeQuery("select uuid, name from user").let {
                val uuidList = mutableMapOf<String, String>()
                while (it.next())
                    uuidList[it.getString("uuid")] = it.getString("name")
                uuidList.forEach { (uuid, name) ->
                    addBatch("insert into tokens (uuid, name, token) values ('$uuid', '$name', random())")
                }
                executeBatch()
            }
        }
    }

    private fun fileNameSplitter(name: String) : Pair<String, String> {
        name.split("_").let {
            val instant = Instant.ofEpochMilli(it[1].toLong())
            return it[0] to formatter.format(instant)
        }
    }

}