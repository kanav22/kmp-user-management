@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.kanav.usermanager.ui.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanav.usermanager.domain.model.User
import com.kanav.usermanager.ui.components.UserCard
import com.kanav.usermanager.ui.components.UserDetailPane
import kotlinx.coroutines.launch

@Composable
fun AdaptiveUserListLayout(
    users: List<User>,
    selectedUser: User?,
    pendingDeleteUserId: Long?,
    onUserSelected: (User) -> Unit,
    onUserDeselected: () -> Unit,
    onLongPress: (User) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator()
    val scope = rememberCoroutineScope()

    PlatformBackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
        onUserDeselected()
    }

    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                UserListPane(
                    users = users,
                    pendingDeleteUserId = pendingDeleteUserId,
                    contentPadding = contentPadding,
                    listState = listState,
                    onUserClick = { user ->
                        onUserSelected(user)
                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
                    },
                    onUserLongClick = onLongPress,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                UserDetailPane(user = selectedUser)
            }
        },
    )
}

@Composable
fun UserListPane(
    users: List<User>,
    pendingDeleteUserId: Long?,
    contentPadding: PaddingValues,
    listState: LazyListState,
    onUserClick: (User) -> Unit,
    onUserLongClick: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        state = listState,
    ) {
        items(users, key = { it.id }) { user ->
            AnimatedVisibility(
                visible = user.id != pendingDeleteUserId,
                enter = slideInVertically(),
                exit = slideOutHorizontally() + shrinkVertically(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                UserCard(
                    user = user,
                    onClick = { onUserClick(user) },
                    onLongClick = { onUserLongClick(user) },
                )
            }
        }
    }
}
