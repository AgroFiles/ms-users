package com.example.routes

import com.example.models.User
import com.example.models.UserEntity
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.userRouting() {
    routing {
        route("/users") {
            post {
                try {
                    val req = call.receive<User>()

                    val savedUser = transaction {
                        UserEntity.new {
                            name = req.name
                            email = req.email
                        }.toUser()
                    }

                    call.respond(HttpStatusCode.Created, savedUser)
                } catch (e: ContentTransformationException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid JSON format.")
                    )
                }
            }

            get {
                val users = transaction {
                    UserEntity.all().map { it.toUser() }
                }
                call.respond(users)
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                } else {
                    val user = transaction {
                        UserEntity.findById(id)?.toUser()
                    }
                    if (user == null) {
                        call.respond(HttpStatusCode.NotFound, "User not found")
                    } else {
                        call.respond(user)
                    }
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                } else {
                    val deleted = transaction {
                        UserEntity.findById(id)?.delete() != null
                    }

                    if (deleted) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "User not found")
                    }
                }
            }
        }
    }
}