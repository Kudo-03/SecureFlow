package com.example.secureflow.FireBase

<<<<<<< HEAD
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


=======
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
>>>>>>> try5
}
