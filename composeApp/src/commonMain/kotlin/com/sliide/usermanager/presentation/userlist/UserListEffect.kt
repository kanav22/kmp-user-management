package com.sliide.usermanager.presentation.userlist

import com.sliide.usermanager.domain.model.User

sealed interface UserListEffect {
    data class  ShowDeleteSnackbar(val user: User) : UserListEffect
    data class  ShowError(val message: String)     : UserListEffect
    data object UserAddedSuccess                   : UserListEffect
}
