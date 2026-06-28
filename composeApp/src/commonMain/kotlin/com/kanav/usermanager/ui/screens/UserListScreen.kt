package com.kanav.usermanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.kanav.usermanager.presentation.userlist.UserListEffect
import com.kanav.usermanager.presentation.userlist.UserListIntent
import com.kanav.usermanager.presentation.userlist.UserListViewModel
import com.kanav.usermanager.ui.components.EmptyStateContent
import com.kanav.usermanager.ui.components.ErrorStateContent
import com.kanav.usermanager.ui.components.InlineErrorBanner
import com.kanav.usermanager.ui.components.ShimmerUserCard
import com.kanav.usermanager.ui.util.AdaptiveUserListLayout
import com.kanav.usermanager.ui.util.isScrollingUp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(viewModel: UserListViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAddUserSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UserListEffect.ShowDeleteSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "User deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> viewModel.process(UserListIntent.UndoDelete(effect.user))
                        SnackbarResult.Dismissed       -> viewModel.process(UserListIntent.FinalizeDelete)
                    }
                }
                is UserListEffect.ShowError    -> snackbarHostState.showSnackbar(effect.message)
                UserListEffect.UserAddedSuccess -> {
                    showAddUserSheet = false
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Users") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddUserSheet = true },
                expanded = listState.isScrollingUp(),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add new user") },
                text = { Text("Add User") },
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.users.isEmpty() -> {
                LazyColumn(contentPadding = padding) {
                    items(8) { ShimmerUserCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
                }
            }
            state.error != null && state.users.isEmpty() -> {
                ErrorStateContent(
                    error = state.error,
                    onRetry = { viewModel.process(UserListIntent.LoadUsers) },
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.process(UserListIntent.RefreshUsers) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        if (state.error != null) {
                            InlineErrorBanner(state.error)
                        }
                        if (state.users.isEmpty() && !state.isLoading) {
                            EmptyStateContent()
                        } else {
                            AdaptiveUserListLayout(
                                users = state.users,
                                selectedUser = state.selectedUser,
                                pendingDeleteUserId = state.pendingDeleteUser?.id,
                                onUserSelected = { viewModel.process(UserListIntent.SelectUser(it)) },
                                onUserDeselected = { viewModel.process(UserListIntent.ClearSelectedUser) },
                                onLongPress = { user ->
                                    if (state.pendingDeleteUser == null) {
                                        viewModel.process(UserListIntent.RequestDelete(user))
                                    }
                                },
                                listState = listState,
                            )
                        }
                    }
                }
            }
        }
    }

    state.showDeleteDialogFor?.let { user ->
        AlertDialog(
            onDismissRequest = { viewModel.process(UserListIntent.DismissDeleteDialog) },
            title = { Text("Delete ${user.name}?") },
            text  = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.process(UserListIntent.ConfirmDelete(user)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.process(UserListIntent.DismissDeleteDialog) }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showAddUserSheet) {
        AddUserBottomSheet(
            formState = state.formState,
            onIntent  = viewModel::process,
            onDismiss = {
                showAddUserSheet = false
                viewModel.process(UserListIntent.DismissAddUserSheet)
            },
        )
    }
}
