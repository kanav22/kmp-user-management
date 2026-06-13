package com.sliide.usermanager.presentation.userlist

import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.model.UserStatus
import com.sliide.usermanager.domain.repository.UserListError

data class UserListState(
    val users: List<User>           = emptyList(),
    val isLoading: Boolean          = false,
    val isRefreshing: Boolean       = false,
    val error: UserListError?       = null,
    val showDeleteDialogFor: User?  = null,
    val pendingDeleteUser: User?    = null,
    val selectedUser: User?         = null,
    val formState: AddUserFormState = AddUserFormState(),
)

data class AddUserFormState(
    val name: String          = "",
    val email: String         = "",
    val gender: Gender        = Gender.MALE,
    val status: UserStatus    = UserStatus.ACTIVE,
    val nameError: String?    = null,
    val emailError: String?   = null,
    val submitError: String?  = null,
    val isSubmitting: Boolean = false,
)
