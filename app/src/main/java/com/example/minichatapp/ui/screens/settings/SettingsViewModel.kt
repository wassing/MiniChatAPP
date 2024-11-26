package com.example.minichatapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minichatapp.data.local.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings
) : ViewModel() {
    val serverHost: StateFlow<String> = appSettings.serverHost
        .stateIn(viewModelScope, SharingStarted.Eagerly, "10.0.2.2")

    val serverPort: StateFlow<Int> = appSettings.serverPort
        .stateIn(viewModelScope, SharingStarted.Eagerly, 8080)

    val reconnectInterval: StateFlow<Long> = appSettings.reconnectInterval
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5000L)

    fun updateSettings(host: String, port: Int, interval: Long) {
        viewModelScope.launch {
            appSettings.updateServerHost(host)
            appSettings.updateServerPort(port)
            appSettings.updateReconnectInterval(interval)
        }
    }
}