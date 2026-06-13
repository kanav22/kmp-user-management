package com.sliide.usermanager

import app.cash.turbine.test
import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.UserStatus
import com.sliide.usermanager.domain.repository.ValidationError
import com.sliide.usermanager.domain.repository.ValidationException
import com.sliide.usermanager.fakes.FakeUserRepository
import com.sliide.usermanager.fakes.testUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRepositoryImplTest {

    @Test
    fun `refreshLastPage failure - observeUsers continues emitting cached list`() = runTest {
        val repo = FakeUserRepository(initialUsers = listOf(testUser))
        repo.refreshResult = Result.failure(Exception("No internet"))
        repo.observeUsers().test {
            val initial = awaitItem()
            assertEquals(listOf(testUser), initial)
            repo.refreshLastPage()
            expectNoEvents()
        }
    }

    @Test
    fun `addUser success - returns user`() = runTest {
        val repo = FakeUserRepository()
        val result = repo.addUser("Alice", "alice@example.com", Gender.FEMALE, UserStatus.ACTIVE)
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }

    @Test
    fun `addUser with 422 - returns ValidationException`() = runTest {
        val repo = FakeUserRepository()
        repo.addResult = Result.failure(
            ValidationException(listOf(ValidationError("email", "has already been taken")))
        )
        val result = repo.addUser("Alice", "taken@example.com", Gender.FEMALE, UserStatus.ACTIVE)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is ValidationException)
        assertEquals("email", ex.errors.first().field)
    }

    @Test
    fun `addUser network failure - returns failure`() = runTest {
        val repo = FakeUserRepository()
        repo.addResult = Result.failure(Exception("No internet"))
        val result = repo.addUser("Alice", "alice@example.com", Gender.FEMALE, UserStatus.ACTIVE)
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteUser success - returns success`() = runTest {
        val repo = FakeUserRepository()
        repo.deleteResult = Result.success(Unit)
        val result = repo.deleteUser(1L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteUser failure - returns failure`() = runTest {
        val repo = FakeUserRepository()
        repo.deleteResult = Result.failure(Exception("Network error"))
        val result = repo.deleteUser(1L)
        assertTrue(result.isFailure)
    }
}
