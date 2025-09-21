package com.example.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Users : IntIdTable() {
    val name = varchar("name", 50)
    val email = varchar("email", 100).uniqueIndex()
    val creationDate = datetime("creation_date").clientDefault { LocalDateTime.now() }
}

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(Users)
    var name by Users.name
    var email by Users.email
    var creationDate by Users.creationDate

    fun toUser() = User(
        id = id.value,
        name = name,
        email = email,
        creationDate = creationDate.toString()
    )
}

@kotlinx.serialization.Serializable
data class User(
    val id: Int? = null,
    val name: String,
    val email: String,
    val creationDate: String? = null
)