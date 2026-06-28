package com.kanav.usermanager.domain.usecase

import com.kanav.usermanager.domain.repository.UserRepository

fun interface DeleteUserUseCase {
    suspend operator fun invoke(userId: Long): Result<Unit>
}

class DeleteUserUseCaseImpl(
    private val repository: UserRepository,
) : DeleteUserUseCase {
    override suspend fun invoke(userId: Long): Result<Unit> = repository.deleteUser(userId)
}
