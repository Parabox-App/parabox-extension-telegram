package com.ojhdtapp.parabox.extension.telegram.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.extension.telegram.core.util.DataStoreKeys
import com.ojhdtapp.parabox.extension.telegram.core.util.dataStore
import com.ojhdtapp.parabox.extension.telegram.domain.telegram.Authentication
import com.ojhdtapp.parabox.extension.telegram.domain.telegram.TelegramClient
import com.ojhdtapp.parabox.extension.telegram.domain.util.ServiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val client: TelegramClient,
) : ViewModel() {
    // UiEvent
    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()
    fun emitToUiEventFlow(event: UiEvent) {
        viewModelScope.launch {
            _uiEventFlow.emit(event)
        }
    }

    // MainApp Installation
    private val _isMainAppInstalled = MutableStateFlow(false)
    val isMainAppInstalled get() = _isMainAppInstalled.asStateFlow()
    fun setMainAppInstalled(isInstalled: Boolean) {
        viewModelScope.launch {
            _isMainAppInstalled.emit(isInstalled)
        }
    }

    // Service Status
    private val _serviceStatusStateFlow = MutableStateFlow<ServiceStatus>(ServiceStatus.Stop)
    val serviceStatusStateFlow = _serviceStatusStateFlow.asStateFlow()
    fun updateServiceStatusStateFlow(value: ServiceStatus) {
        viewModelScope.launch {
            _serviceStatusStateFlow.emit(value)
        }
    }

    // Auto Login Switch
    val autoLoginSwitchFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.AUTO_LOGIN] ?: false
        }

    fun setAutoLoginSwitch(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.AUTO_LOGIN] = value
            }
        }
    }

    //Foreground Service
    val foregroundServiceSwitchFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.FOREGROUND_SERVICE] ?: true
        }

    fun setForegroundServiceSwitch(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.FOREGROUND_SERVICE] = value
            }
        }
    }

    // Login
    private val _loginState = mutableStateOf<LoginState>(LoginState.Loading)

    val loginState: State<LoginState> get() = _loginState

    init {
        client.authState.onEach {
            Log.d("MainViewModel", "authState: $it")
            when (it) {
                Authentication.UNAUTHENTICATED -> {
                    _loginState.value = LoginState.Unauthenticated
                }
                Authentication.UNKNOWN -> {
                    _loginState.value = LoginState.Unauthenticated
                }
                Authentication.WAIT_FOR_NUMBER -> {
                    _loginState.value = LoginState.InsertNumber()
                }
                Authentication.WAIT_FOR_CODE -> {
                    _loginState.value = LoginState.InsertCode()
                }
                Authentication.WAIT_FOR_PASSWORD -> {
                    _loginState.value = LoginState.InsertPassword()
                }
                Authentication.AUTHENTICATED -> {
                    _loginState.value = LoginState.Authenticated
                }
            }
        }.launchIn(viewModelScope)
    }

    fun startAuthentication() {
        _loginState.value = LoginState.Loading
        client.startAuthentication()
    }

    fun insertPhoneNumber(number: String) {
        _loginState.value = LoginState.Loading
        client.insertPhoneNumber(number)
    }

    fun insertCode(code: String) {
        _loginState.value = LoginState.Loading
        client.insertCode(code)
    }

    fun insertPassword(password: String) {
        _loginState.value = LoginState.Loading
        client.insertPassword(password)
    }
}

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
}

sealed class LoginState {
    object Loading : LoginState()
    data class InsertNumber(val previousError: Throwable? = null) : LoginState()
    data class InsertCode(val previousError: Throwable? = null) : LoginState()
    data class InsertPassword(val previousError: Throwable? = null) : LoginState()
    object Authenticated : LoginState()
    object Unauthenticated : LoginState()
}