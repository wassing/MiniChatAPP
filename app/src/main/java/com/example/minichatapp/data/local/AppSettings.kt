package com.example.minichatapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AppSettings @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val SERVER_HOST = stringPreferencesKey("server_host")
        val SERVER_PORT = intPreferencesKey("server_port")
        val RECONNECT_INTERVAL = longPreferencesKey("reconnect_interval")
    }

    val serverHost: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SERVER_HOST] ?: "10.0.2.2"
        }

    val serverPort: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SERVER_PORT] ?: 8080
        }

    val reconnectInterval: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.RECONNECT_INTERVAL] ?: 5000L // 默认5秒
        }

    suspend fun updateServerHost(host: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_HOST] = host
        }
    }

    suspend fun updateServerPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_PORT] = port
        }
    }

    suspend fun updateReconnectInterval(interval: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECONNECT_INTERVAL] = interval
        }
    }
}