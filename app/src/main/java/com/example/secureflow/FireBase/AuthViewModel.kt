package com.example.secureflow.FireBase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth


import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AuthViewModel(private val auth: FirebaseAuth) : ViewModel() {

    // State
    var state by mutableStateOf(AuthState())
        private set

    // Events
    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> {
                state = state.copy(email = event.email)
            }
            is AuthEvent.PasswordChanged -> {
                state = state.copy(password = event.password)
            }
            is AuthEvent.SignIn -> signIn()
            is AuthEvent.SignUp -> signUp()
            AuthEvent.SignOut -> signOut()
        }
    }

    private fun signIn() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                auth.signInWithEmailAndPassword(state.email, state.password).await()
                state = state.copy(isSuccess = true)
            } catch (e: Exception) {
                state = state.copy(error = e.localizedMessage ?: "Login failed")
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    private fun signUp() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                auth.createUserWithEmailAndPassword(state.email, state.password).await()
                state = state.copy(isSuccess = true)
            } catch (e: Exception) {
                state = state.copy(error = e.localizedMessage ?: "Sign-up failed")
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    private fun signOut() {
        auth.signOut()
        state = AuthState()
    }


}
