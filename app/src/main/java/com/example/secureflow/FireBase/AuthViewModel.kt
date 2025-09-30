package com.example.secureflow.FireBase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.firestore

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>(AuthState.Unauthenticated)
    val authState: LiveData<AuthState> = _authState

    val currentUser get() = auth.currentUser

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        _authState.value = if (auth.currentUser != null)
            AuthState.Authenticated else AuthState.Unauthenticated
    }

    // --- LOGIN ---
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    // --- SIGNUP ---
    fun signup(email: String, password: String, firstName: String, lastName: String) {
        if (email.isBlank() || password.isBlank() || firstName.isBlank() || lastName.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }

        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Signup failed")
                    return@addOnCompleteListener
                }

                val user = auth.currentUser ?: return@addOnCompleteListener

                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName("$firstName $lastName")
                    .build()
                user.updateProfile(profileUpdates)

                // Save user info to Firestore (optional)
                val userData = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email
                )
                Firebase.firestore.collection("users").document(user.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        _authState.value = AuthState.Authenticated
                    }
                    .addOnFailureListener {
                        // Even if Firestore fails, still allow login
                        _authState.value = AuthState.Authenticated
                    }
            }
    }

    // --- SIGNOUT ---
    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    // --- GETTERS ---
    fun getUserId(): String? = auth.currentUser?.uid

    fun getUserName(onResult: (String?) -> Unit) {
        val uid = getUserId() ?: return onResult(null)
        Firebase.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last = doc.getString("lastName") ?: ""
                val name = "$first $last".trim().ifBlank { null }
                onResult(name)
            }
            .addOnFailureListener { onResult(null) }
    }
}
