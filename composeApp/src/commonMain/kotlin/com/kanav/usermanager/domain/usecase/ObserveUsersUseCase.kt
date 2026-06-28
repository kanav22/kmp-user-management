package com.kanav.usermanager.domain.usecase

import com.kanav.usermanager.domain.model.User
import com.kanav.usermanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

fun interface ObserveUsersUseCase {
    operator fun invoke(): Flow<List<User>>
}

class ObserveUsersUseCaseImpl(
    private val repository: UserRepository,
) : ObserveUsersUseCase {
    override fun invoke(): Flow<List<User>> = repository.observeUsers()
}
