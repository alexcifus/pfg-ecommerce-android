package com.example.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.User
import com.example.myapplication.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    var token: String? = null
        private set

    private var sessionVersion = 0L

    fun logout() {
        sessionVersion++
        viewModelScope.coroutineContext.cancelChildren()
        token = null
        _user.value = null
        _error.value = null
        _loading.value = false
    }

    fun login(email: String, password: String) {
        val version = sessionVersion
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val response = ApiClient.api.loginMobile(
                    LoginRequest(email = email, password = password)
                )
                coroutineContext.ensureActive()
                if (version != sessionVersion) return@launch
                token = response.accessToken
                _user.value = response.user
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version != sessionVersion) return@launch
                _error.value = e.message ?: "Error de login"
            } finally {
                if (version == sessionVersion) _loading.value = false
            }
        }
    }
}
