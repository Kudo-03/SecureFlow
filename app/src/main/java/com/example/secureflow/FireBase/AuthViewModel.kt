package com.example.secureflow.FireBase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.firestore

class AuthViewModel : ViewModel() {

    val currentUser: FirebaseUser?
        get() = auth.currentUser
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        _authState.value = if (auth.currentUser == null) {
            AuthState.Unauthenticated
        } else {
            AuthState.Authenticated
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }
        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _authState.value = if (task.isSuccessful) {
                    AuthState.Authenticated
                } else {
                    AuthState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signup(email: String, password: String, firstName: String, lastName: String) {
        val fullName = "$firstName $lastName"
        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()

                        user.updateProfile(profileUpdates).addOnCompleteListener {
                            val userData = hashMapOf(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "email" to email
                            )
                            Firebase.firestore.collection("users").document(user.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    _authState.value = AuthState.Authenticated
                                }
                                .addOnFailureListener { e ->
                                    _authState.value = AuthState.Error(e.message ?: "Failed to save user data")
                                }
                        }
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Signup failed")
                }
            }
    }

    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun getUserId(): String? = auth.currentUser?.uid

    fun getUserName(onResult: (String?) -> Unit) {
        val uid = getUserId()
        if (uid == null) {
            onResult(null)
            return
        }

            Firebase.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val fullName = "$firstName $lastName".trim()
                onResult(if (fullName.isBlank()) null else fullName)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}