package com.sliide.usermanager.domain.model

import kotlinx.datetime.Instant

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val gender: Gender,
    val status: UserStatus,
    val addedAt: Instant,
)

enum class Gender {
    MALE, FEMALE;

    val apiValue: String get() = name.lowercase()
}

enum class UserStatus {
    ACTIVE, INACTIVE;

    val apiValue: String get() = name.lowercase()
}
