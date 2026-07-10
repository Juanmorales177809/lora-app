package com.example.reservas.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    fun notifyUnauthorized() {
        _sessionExpired.value = true
    }

    fun markAuthenticated() {
        _sessionExpired.value = false
    }
}