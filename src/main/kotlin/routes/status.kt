package com.example.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.statusRouting() {
    routing {
        route("/status") {
            get {
                val dbOk = try {
                    transaction {
                        pingDb()
                    }
                    true
                } catch (_: Exception) {
                    false
                }

                val status = if (dbOk) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
                val body = """{"status":"${if (dbOk) "OK" else "DEGRADED"}","db":"${if (dbOk) "UP" else "DOWN"}"}"""
                call.respondText(body, ContentType.Application.Json, status)
            }
        }
    }
}

private fun Transaction.pingDb(): Boolean {
    return exec("SELECT 1") { rs -> rs.next() } ?: false
}