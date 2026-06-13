package com.sliide.usermanager.fakes

import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.model.UserStatus
import com.sliide.usermanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

val testUser = User(
    id = 1L,
    name = "Alice",
    email = "alice@example.com",
    gender = Gender.FEMALE,
    status = UserStatus.ACTIVE,
    addedAt = Clock.System.now(),
)

class FakeUserRepository(initialUsers: List<User> = emptyList()) : UserRepository {
    private val _users = MutableStateFlow(initialUsers)

    var refreshResult: Result<Unit> = Result.success(Unit)
    var addResult:     Result<User> = Result.success(testUser)
    var deleteResult:  Result<Unit> = Result.success(Unit)

    override fun observeUsers(): Flow<List<User>> = _users

    override suspend fun refreshLastPage(): Result<Unit> = refreshResult

    override suspend fun addUser(
        name: String, email: String, gender: Gender, status: UserStatus,
    ): Result<User> = addResult

    override suspend fun deleteUser(id: Long): Result<Unit> = deleteResult

    fun emit(users: List<User>) {
        _users.value = users
    }
}
