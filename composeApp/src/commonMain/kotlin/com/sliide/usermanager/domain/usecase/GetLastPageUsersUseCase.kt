package com.sliide.usermanager.domain.usecase

import com.sliide.usermanager.domain.repository.UserRepository

fun interface GetLastPageUsersUseCase {
    suspend operator fun invoke(): Result<Unit>
}

class GetLastPageUsersUseCaseImpl(
    private val repository: UserRepository,
) : GetLastPageUsersUseCase {
    override suspend fun invoke(): Result<Unit> = repository.refreshLastPage()
}
