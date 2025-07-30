package com.example.secureflow.FireBase

sealed class AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent()
    data class PasswordChanged(val password: String) : AuthEvent()
    object SignIn : AuthEvent()
    object SignUp : AuthEvent()
    object SignOut : AuthEvent()
}
