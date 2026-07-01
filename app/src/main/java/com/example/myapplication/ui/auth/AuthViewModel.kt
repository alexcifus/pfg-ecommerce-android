package com.example.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.User
import com.example.myapplication.network.ApiClient
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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val response = ApiClient.api.loginMobile(
                    LoginRequest(email = email, password = password)
                )
                token = response.accessToken
                _user.value = response.user
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de login"
            } finally {
                _loading.value = false
            }
        }
    }
}
