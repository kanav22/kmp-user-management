package com.sliide.usermanager.data.remote

import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.model.UserStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val gender: String,
    val status: String,
) {
    fun toDomain(addedAt: Instant): User = User(
        id = id,
        name = name,
        email = email,
        gender = if (gender.lowercase() == "female") Gender.FEMALE else Gender.MALE,
        status = if (status.lowercase() == "inactive") UserStatus.INACTIVE else UserStatus.ACTIVE,
        addedAt = addedAt,
    )
}

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val gender: String,
    val status: String,
)

@Serializable
data class ApiValidationError(
    val field: String,
    val message: String,
)
