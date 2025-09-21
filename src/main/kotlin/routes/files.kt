package com.example.routes

import com.example.models.File
import com.example.models.FileEntity
import com.example.models.UserEntity
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

private fun ApplicationCall.currentUserIdOrNull(): Int? =
    request.headers["user-id"]?.toIntOrNull()

fun Application.fileRouting() {
    routing {
        route("/files") {
            // Create
            post {
                val currentUserId = call.currentUserIdOrNull()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid user-id"))
                    return@post
                }

                val req = try { call.receive<File>() } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid JSON"))
                    return@post
                }

                if (req.ownerId != currentUserId) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only owner can create file for themselves"))
                    return@post
                }

                val created = transaction {
                    val owner = UserEntity.findById(req.ownerId) ?: return@transaction null

                    val file = FileEntity.new {
                        url = req.url
                        this.owner = owner
                        this.estado = 1 // activo
                    }

                    val normalizedReaderIds = req.readerIds.distinct().filter { it != owner.id.value }
                    val readers = normalizedReaderIds.mapNotNull { UserEntity.findById(it) }
                    if (readers.isNotEmpty()) file.readers = SizedCollection(readers)

                    val normalizedWriterIds = req.writerIds.distinct().filter { it != owner.id.value }
                    val writers = normalizedWriterIds.mapNotNull { UserEntity.findById(it) }
                    if (writers.isNotEmpty()) file.writers = SizedCollection(writers)

                    file.toFile()
                }

                if (created == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Owner not found"))
                } else {
                    call.respond(HttpStatusCode.Created, created)
                }
            }

            get("/{id}") {
                val currentUserId = call.currentUserIdOrNull()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid user-id"))
                    return@get
                }

                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                    return@get
                }

                val result = transaction {
                    val file = FileEntity.findById(id)
                        ?: return@transaction Pair(HttpStatusCode.NotFound, mapOf("error" to "File not found"))

                    if (file.estado != 1) {
                        return@transaction Pair(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
                    }

                    val ownerId = file.owner.id.value
                    val readerIds = file.readers.map { it.id.value }
                    val canAccess = currentUserId == ownerId || readerIds.contains(currentUserId)
                    if (!canAccess) {
                        return@transaction Pair(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    }

                    Pair(HttpStatusCode.OK, file.toFile())
                }

                call.respond(result.first, result.second)
            }

            put("/{id}") {
                val currentUserId = call.currentUserIdOrNull()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid user-id"))
                    return@put
                }

                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                    return@put
                }

                val req = try { call.receive<File>() } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid JSON"))
                    return@put
                }

                val updated = transaction {
                    val file = FileEntity.findById(id)
                        ?: return@transaction Pair(HttpStatusCode.NotFound, mapOf("error" to "File not found"))

                    if (file.estado != 1) {
                        return@transaction Pair(HttpStatusCode.Conflict, mapOf("error" to "File is inactive"))
                    }

                    val ownerId = file.owner.id.value
                    val writerIds = file.writers.map { it.id.value }
                    val canModify = currentUserId == ownerId || writerIds.contains(currentUserId)
                    if (!canModify) {
                        return@transaction Pair(HttpStatusCode.Forbidden, mapOf("error" to "Only owner or writer can update"))
                    }

                    file.url = req.url

                    // Si también actualizas listas:
                    val normalizedReaderIds = req.readerIds.distinct().filter { it != ownerId }
                    val readers = normalizedReaderIds.mapNotNull { UserEntity.findById(it) }
                    file.readers = SizedCollection(readers)

                    val normalizedWriterIds = req.writerIds.distinct().filter { it != ownerId }
                    val writers = normalizedWriterIds.mapNotNull { UserEntity.findById(it) }
                    file.writers = SizedCollection(writers)

                    Pair(HttpStatusCode.OK, file.toFile())
                }

                call.respond(updated.first, updated.second)
            }

            delete("/{id}") {
                val currentUserId = call.currentUserIdOrNull()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid user-id"))
                    return@delete
                }

                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                    return@delete
                }

                val result = transaction {
                    val file = FileEntity.findById(id)
                        ?: return@transaction Pair(HttpStatusCode.NotFound, mapOf("error" to "File not found"))

                    val ownerId = file.owner.id.value
                    if (ownerId != currentUserId) {
                        return@transaction Pair(HttpStatusCode.Forbidden, mapOf("error" to "Only owner can delete"))
                    }

                    if (file.estado == 0) {
                        return@transaction Pair(HttpStatusCode.NoContent, Unit)
                    }

                    file.estado = 0
                    Pair(HttpStatusCode.NoContent, Unit)
                }

                call.respond(result.first, result.second)
            }
        }
    }
}
