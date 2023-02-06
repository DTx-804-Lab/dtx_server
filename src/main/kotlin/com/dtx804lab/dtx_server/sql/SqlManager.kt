package com.dtx804lab.dtx_server.sql

import java.lang.StringBuilder
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

object SqlManager {

    private lateinit var pdDatabase: Connection

    fun init() {
        val statement = pdDatabase.createStatement()
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

    fun clear() {
        val statement = pdDatabase.createStatement()
        statement.queryTimeout = 30
        statement.execute("drop table if exists user")
    }

    fun registerPD(id: Int, name: String): UUID {
        val statement = pdDatabase.createStatement()
        statement.queryTimeout = 30
        println("$id: $name")
        val result = statement.executeQuery("select patientID, name from user where patientId = $id")
        if (result.next()) return UUID(0, 0)
        val uuid = UUID.randomUUID()
        statement.executeUpdate("insert into user (patientID, name, uuid) values ($id, '$name', '$uuid')")
        return uuid
    }

    fun loginPD(id: Int, name: String): UUID {
        val statement = pdDatabase.createStatement()
        statement.queryTimeout = 30
        println("$id: $name")
        val result = statement.executeQuery("select patientID, name, uuid from user where patientId = $id")
        return if (result.next() && result.getString("name") == name) UUID.fromString(result.getString("uuid"))
        else UUID(0, 0)
    }

    fun start() {
        pdDatabase = DriverManager.getConnection("jdbc:sqlite:pd.db")
    }

    fun close() {
        pdDatabase.close()
    }

    fun UUID.string32(): String = toString().replace("-", "")

}