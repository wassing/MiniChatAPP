package com.example.minichatapp.ui.screens.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minichatapp.data.repository.ContactRepository
import com.example.minichatapp.domain.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    init {
        // 加载联系人列表
        viewModelScope.launch {
            contactRepository.getAllContacts()
                .collect { contacts ->
                    _uiState.update { it.copy(contacts = contacts) }
                }
        }
    }

    fun addContact(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isAddingContact = true,
                addContactError = null
            )}

            try {
                contactRepository.addContact(username)
                    .onSuccess {
                        _uiState.update { it.copy(
                            showAddContactDialog = false,
                            addContactError = null
                        )}
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(
                            addContactError = error.message
                        )}
                    }
            } finally {
                _uiState.update { it.copy(isAddingContact = false) }
            }
        }
    }

    fun showAddContactDialog() {
        _uiState.update { it.copy(
            showAddContactDialog = true,
            addContactError = null
        )}
    }

    fun hideAddContactDialog() {
        _uiState.update { it.copy(
            showAddContactDialog = false,
            addContactError = null
        )}
    }

    fun clearUnreadCount(username: String) {
        viewModelScope.launch {
            contactRepository.clearUnreadCount(username)
        }
    }

    data class ContactUiState(
        val contacts: List<Contact> = emptyList(),
        val showAddContactDialog: Boolean = false,
        val isAddingContact: Boolean = false,
        val addContactError: String? = null
    )
}