package com.kanav.usermanager.domain.usecase

import com.kanav.usermanager.domain.model.Gender
import com.kanav.usermanager.domain.model.User
import com.kanav.usermanager.domain.model.UserStatus
import com.kanav.usermanager.domain.repository.UserRepository

fun interface AddUserUseCase {
    suspend operator fun invoke(name: String, email: String, gender: Gender, status: UserStatus): Result<User>
}

class AddUserUseCaseImpl(
    private val repository: UserRepository,
) : AddUserUseCase {
    override suspend fun invoke(name: String, email: String, gender: Gender, status: UserStatus): Result<User> =
        repository.addUser(name, email, gender, status)
}
