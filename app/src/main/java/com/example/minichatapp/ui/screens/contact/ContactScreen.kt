package com.example.minichatapp.ui.screens.contact

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minichatapp.domain.model.Contact

@Composable
fun ContactScreen(
    onContactClick: (Contact) -> Unit,
    viewModel: ContactViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ContactListScreen(
        contacts = uiState.contacts,
        onContactClick = { contact ->
            // 清除未读消息计数
            viewModel.clearUnreadCount(contact.username)
            onContactClick(contact)
        },
        onAddContactClick = viewModel::showAddContactDialog
    )

    // 添加联系人对话框
    if (uiState.showAddContactDialog) {
        AddContactDialog(
            onDismiss = viewModel::hideAddContactDialog,
            onConfirm = viewModel::addContact,
            isLoading = uiState.isAddingContact,
            errorMessage = uiState.addContactError
        )
    }
}