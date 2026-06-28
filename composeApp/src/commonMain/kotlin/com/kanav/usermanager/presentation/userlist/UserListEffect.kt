package com.kanav.usermanager.presentation.userlist

import com.kanav.usermanager.domain.model.User

sealed interface UserListEffect {
    data class  ShowDeleteSnackbar(val user: User) : UserListEffect
    data class  ShowError(val message: String)     : UserListEffect
    data object UserAddedSuccess                   : UserListEffect
}
