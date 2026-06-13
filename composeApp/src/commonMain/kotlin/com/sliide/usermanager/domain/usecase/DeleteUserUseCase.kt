package com.sliide.usermanager.domain.usecase

import com.sliide.usermanager.domain.repository.UserRepository

fun interface DeleteUserUseCase {
    suspend operator fun invoke(userId: Long): Result<Unit>
}

class DeleteUserUseCaseImpl(
    private val repository: UserRepository,
) : DeleteUserUseCase {
    override suspend fun invoke(userId: Long): Result<Unit> = repository.deleteUser(userId)
}
