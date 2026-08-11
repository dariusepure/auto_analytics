/*
 * Copyright (C) 2026 Darius Epure (Darius DevWorks)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.dariusepure.caractivitylog.data.auth

import com.dariusepure.caractivitylog.domain.User

data class FirestoreUser(
    val id: String = "",
    val email: String = ""
)

fun User.toFirebase() = FirestoreUser(
    id = this.id,
    email = this.email
)

fun FirestoreUser.fromFirebase() = User(
    id = this.id,
    email = this.email
)

