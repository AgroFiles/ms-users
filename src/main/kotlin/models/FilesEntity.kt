package com.example.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Files : IntIdTable() {
    val url = varchar("url", 512)
    val owner = reference("owner_id", Users, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    val estado = integer("estado").default(1)
}

object FileReaders : IntIdTable() {
    val file = reference("file_id", Files, onDelete = ReferenceOption.CASCADE)
    val user = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    init {
        index(true, file, user)
    }
}

object FileWriters : IntIdTable() {
    val file = reference("file_id", Files, onDelete = ReferenceOption.CASCADE)
    val user = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    init {
        index(true, file, user) 
    }
}

class FileEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FileEntity>(Files)

    var url by Files.url
    var owner by UserEntity referencedOn Files.owner
    var createdAt by Files.createdAt
    var updatedAt by Files.updatedAt
    var estado by Files.estado

    var readers by UserEntity via FileReaders
    var writers by UserEntity via FileWriters

    fun toFile() = File(
        id = id.value,
        url = url,
        ownerId = owner.id.value,
        readerIds = readers.map { it.id.value },
        writerIds = writers.map { it.id.value },
        estado = estado,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString()
    )
}

@kotlinx.serialization.Serializable
data class File(
    val id: Int? = null,
    val url: String,
    val ownerId: Int,
    val readerIds: List<Int> = emptyList(),
    val writerIds: List<Int> = emptyList(),
    val estado: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
