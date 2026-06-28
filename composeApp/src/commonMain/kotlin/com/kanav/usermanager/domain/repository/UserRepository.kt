package com.kanav.usermanager.domain.repository

import com.kanav.usermanager.domain.model.Gender
import com.kanav.usermanager.domain.model.User
import com.kanav.usermanager.domain.model.UserStatus
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUsers(): Flow<List<User>>
    suspend fun refreshLastPage(): Result<Unit>
    suspend fun addUser(name: String, email: String, gender: Gender, status: UserStatus): Result<User>
    suspend fun deleteUser(id: Long): Result<Unit>
}

sealed interface UserListError {
    data object NoInternet   : UserListError
    data object Generic      : UserListError
    data object MissingToken : UserListError
}

class ValidationException(val errors: List<ValidationError>) : Exception()
data class ValidationError(val field: String, val message: String)
