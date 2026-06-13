package com.sliide.usermanager.data.remote

import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.repository.ValidationError
import com.sliide.usermanager.domain.repository.ValidationException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.datetime.Clock

class UserApiService(private val client: HttpClient) {

    suspend fun fetchLastPage(): List<UserDto> {
        val probe: HttpResponse = client.get("https://gorest.co.in/public/v2/users") {
            parameter("page", 1)
        }
        val lastPage = probe.headers["X-Pagination-Pages"]?.toIntOrNull() ?: 1
        if (lastPage == 1) return probe.body()
        return client.get("https://gorest.co.in/public/v2/users") {
            parameter("page", lastPage)
        }.body()
    }

    suspend fun createUser(name: String, email: String, gender: String, status: String): User {
        val response: HttpResponse = client.post("https://gorest.co.in/public/v2/users") {
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(name, email, gender, status))
        }
        if (response.status == HttpStatusCode.UnprocessableEntity) {
            val apiErrors = response.body<List<ApiValidationError>>()
            throw ValidationException(apiErrors.map { ValidationError(it.field, it.message) })
        }
        return response.body<UserDto>().toDomain(addedAt = Clock.System.now())
    }

    suspend fun deleteUser(id: Long) {
        client.delete("https://gorest.co.in/public/v2/users/$id")
    }
}
