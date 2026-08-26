package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface AuthRepository {
    val currentUserEmail: Flow<String?>
    val isUserLoggedIn: Flow<Boolean>
    val isFirebaseMode: Boolean
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signup(email: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun loginWithGoogle(idToken: String, email: String): Result<Unit>
}

class FirebaseAuthRepository : AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override val isFirebaseMode: Boolean = true

    override val currentUserEmail: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.email)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val isUserLoggedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun signup(email: String, password: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun loginWithGoogle(idToken: String, email: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }
}

class LocalAuthRepository(context: Context) : AuthRepository {
    private val prefs = context.getSharedPreferences("local_auth_prefs", Context.MODE_PRIVATE)
    private val _currentUserEmail = MutableStateFlow<String?>(prefs.getString("logged_in_email", null))
    private val _isUserLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))

    override val isFirebaseMode: Boolean = false
    override val currentUserEmail: Flow<String?> = _currentUserEmail
    override val isUserLoggedIn: Flow<Boolean> = _isUserLoggedIn

    override suspend fun login(email: String, password: String): Result<Unit> {
        // Simple local registration-on-the-fly or login
        val savedPassword = prefs.getString("user_pwd_$email", null)
        if (savedPassword == null) {
            // Auto register on first login for a smooth demo experience!
            prefs.edit()
                .putString("user_pwd_$email", password)
                .putString("logged_in_email", email)
                .putBoolean("is_logged_in", true)
                .apply()
            _currentUserEmail.value = email
            _isUserLoggedIn.value = true
            return Result.success(Unit)
        } else if (savedPassword == password) {
            prefs.edit()
                .putString("logged_in_email", email)
                .putBoolean("is_logged_in", true)
                .apply()
            _currentUserEmail.value = email
            _isUserLoggedIn.value = true
            return Result.success(Unit)
        } else {
            return Result.failure(Exception("Incorrect password for this local account."))
        }
    }

    override suspend fun signup(email: String, password: String): Result<Unit> {
        val existingPassword = prefs.getString("user_pwd_$email", null)
        if (existingPassword != null) {
            return Result.failure(Exception("Local account already exists. Please log in."))
        }
        prefs.edit()
            .putString("user_pwd_$email", password)
            .putString("logged_in_email", email)
            .putBoolean("is_logged_in", true)
            .apply()
        _currentUserEmail.value = email
        _isUserLoggedIn.value = true
        return Result.success(Unit)
    }

    override suspend fun logout() {
        prefs.edit()
            .remove("logged_in_email")
            .putBoolean("is_logged_in", false)
            .apply()
        _currentUserEmail.value = null
        _isUserLoggedIn.value = false
    }

    override suspend fun loginWithGoogle(idToken: String, email: String): Result<Unit> {
        prefs.edit()
            .putString("logged_in_email", email)
            .putBoolean("is_logged_in", true)
            .apply()
        _currentUserEmail.value = email
        _isUserLoggedIn.value = true
        return Result.success(Unit)
    }
}

internal fun initFirebase(context: Context): FirebaseApp? {
    return try {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseApp.getInstance()
        } else {
            FirebaseApp.initializeApp(context) ?: run {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1088225563978:android:31bf2c538833d889eb9db3")
                    .setApiKey("AIzaSyDJmJYb8iS-RFI5xLGhMlIqaROu1rrbrP4")
                    .setProjectId("finance-ai-cc067")
                    .setStorageBucket("finance-ai-cc067.firebasestorage.app")
                    .setGcmSenderId("1088225563978")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        }
    } catch (e: Exception) {
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:1088225563978:android:31bf2c538833d889eb9db3")
                .setApiKey("AIzaSyDJmJYb8iS-RFI5xLGhMlIqaROu1rrbrP4")
                .setProjectId("finance-ai-cc067")
                .setStorageBucket("finance-ai-cc067.firebasestorage.app")
                .setGcmSenderId("1088225563978")
                .build()
            FirebaseApp.initializeApp(context, options)
        } catch (e2: Exception) {
            null
        }
    }
}

object AuthRepositoryFactory {
    fun create(context: Context): AuthRepository {
        return try {
            val app = initFirebase(context)
            if (app != null) {
                FirebaseAuth.getInstance(app)
                FirebaseAuthRepository()
            } else {
                LocalAuthRepository(context)
            }
        } catch (e: Exception) {
            LocalAuthRepository(context)
        }
    }
}
