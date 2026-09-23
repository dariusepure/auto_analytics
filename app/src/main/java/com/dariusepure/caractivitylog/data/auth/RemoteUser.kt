package com.dariusepure.caractivitylog.data.auth

import com.dariusepure.caractivitylog.domain.User
import kotlinx.serialization.Serializable

@Serializable
data class RemoteUser(
    val id: String = "",
    val email: String = "",
    val name: String = ""
)

fun User.toRemote() = RemoteUser(
    id = this.id,
    email = this.email,
    name = this.name
)

fun RemoteUser.fromRemote() = User(
    id = this.id,
    email = this.email,
    name = this.name
)
