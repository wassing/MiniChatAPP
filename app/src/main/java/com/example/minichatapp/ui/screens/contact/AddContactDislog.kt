package com.example.minichatapp.ui.screens.contact

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.minichatapp.ui.components.CommonTextField
import com.example.minichatapp.ui.theme.MiniChatAppTheme

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加联系人") },
        text = {
            Column {
                CommonTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "用户名",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username) },
                enabled = username.isNotBlank() && !isLoading
            ) {
                Text(if (isLoading) "添加中..." else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// AddContactDialog.kt
@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun AddContactDialogPreview() {
    MiniChatAppTheme {
        AddContactDialog(
            onDismiss = { },
            onConfirm = { },
            isLoading = false,
            errorMessage = null
        )
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun AddContactDialogWithErrorPreview() {
    MiniChatAppTheme {
        AddContactDialog(
            onDismiss = { },
            onConfirm = { },
            isLoading = false,
            errorMessage = "用户不存在"
        )
    }
}