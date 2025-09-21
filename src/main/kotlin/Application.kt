package com.example

import com.example.models.Users
import com.example.routes.statusRouting
import com.example.routes.userRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import io.github.cdimascio.dotenv.Dotenv

fun main(args: Array<String>) {
    val dotenv = Dotenv.load()

    embeddedServer(Netty, port = 8080) {
        module(dotenv)
    }.start(wait = true)
}

fun Application.module(dotenv: Dotenv) {
    val dbHost = dotenv["DB_HOST"]
    val dbPort = dotenv["DB_PORT"]
    val dbName = dotenv["DB_NAME"]
    val dbUser = dotenv["DB_USER"] ?: "a"
    val dbPass = dotenv["DB_PASSWORD"] ?: "a"

    Database.connect(
        url = "jdbc:postgresql://$dbHost:$dbPort/$dbName",
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPass
    )

    transaction {
        SchemaUtils.create(Users)
    }

    install(ContentNegotiation) { json() }

    routing {
        statusRouting()
        userRouting()
    }
}