package com.dariusepure.caractivitylog.data.auth

import com.dariusepure.caractivitylog.domain.User
import kotlinx.serialization.Serializable

@Serializable
data class FirestoreUser(
    val id: String = "",
    val email: String = "",
    val name: String = ""
)

fun User.toFirebase() = FirestoreUser(
    id = this.id,
    email = this.email,
    name = this.name
)

fun FirestoreUser.fromFirebase() = User(
    id = this.id,
    email = this.email,
    name = this.name
)

