package com.kanav.usermanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kanav.usermanager.domain.model.Gender
import com.kanav.usermanager.domain.model.UserStatus
import com.kanav.usermanager.presentation.userlist.AddUserFormState
import com.kanav.usermanager.presentation.userlist.UserListIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserBottomSheet(
    formState: AddUserFormState,
    onIntent: (UserListIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text("Add User", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = formState.name,
                onValueChange = { onIntent(UserListIntent.UpdateFormName(it)) },
                label = { Text("Full Name") },
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = formState.email,
                onValueChange = { onIntent(UserListIntent.UpdateFormEmail(it)) },
                label = { Text("Email") },
                isError = formState.emailError != null,
                supportingText = formState.emailError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))

            Text("Gender", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Gender.entries.forEachIndexed { index, gender ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Gender.entries.size),
                        onClick = { onIntent(UserListIntent.UpdateFormGender(gender)) },
                        selected = formState.gender == gender,
                    ) {
                        Text(gender.name.lowercase().replaceFirstChar { it.uppercaseChar() })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                UserStatus.entries.forEachIndexed { index, status ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = UserStatus.entries.size),
                        onClick = { onIntent(UserListIntent.UpdateFormStatus(status)) },
                        selected = formState.status == status,
                    ) {
                        Text(status.name.lowercase().replaceFirstChar { it.uppercaseChar() })
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onIntent(UserListIntent.SubmitAddUser) },
                enabled = !formState.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Add User")
                }
            }

            if (formState.submitError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formState.submitError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
