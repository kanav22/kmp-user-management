package com.sliide.usermanager.presentation.userlist

import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.model.UserStatus

sealed interface UserListIntent {
    data object LoadUsers           : UserListIntent
    data object RefreshUsers        : UserListIntent
    data class  RequestDelete(val user: User) : UserListIntent
    data object DismissDeleteDialog : UserListIntent
    data class  ConfirmDelete(val user: User) : UserListIntent
    data class  UndoDelete(val user: User)    : UserListIntent
    data object FinalizeDelete      : UserListIntent
    data class  SelectUser(val user: User)    : UserListIntent
    data object ClearSelectedUser   : UserListIntent
    data class  UpdateFormName(val value: String)     : UserListIntent
    data class  UpdateFormEmail(val value: String)    : UserListIntent
    data class  UpdateFormGender(val value: Gender)   : UserListIntent
    data class  UpdateFormStatus(val value: UserStatus) : UserListIntent
    data object SubmitAddUser       : UserListIntent
    data object DismissAddUserSheet : UserListIntent
}
