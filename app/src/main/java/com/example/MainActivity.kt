package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ProjectRepository
import com.example.data.local.AppDatabase
import com.example.data.remote.FirestoreProjectRepository
import com.example.ui.EditorViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutGoogle
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class AppScreen {
    LANDING,
    EDITOR
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MotionCutAppRoot()
            }
        }
    }
}

@Composable
fun MotionCutAppRoot(
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var continueWithoutSignIn by rememberSaveable { mutableStateOf(false) }
    val user = currentUser

    if (user != null) {
        key(user.uid) {
            AuthenticatedMotionCutApp(currentUser = user)
        }
    } else if (continueWithoutSignIn) {
        key("guest_local_session") {
            GuestMotionCutApp(
                onSignInRequested = { continueWithoutSignIn = false }
            )
        }
    } else {
        AuthScreen(
            onAuthSuccess = { /* authStateFlow automatically transitions to authenticated UI */ },
            onContinueWithoutSignIn = { continueWithoutSignIn = true },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AuthenticatedMotionCutApp(
    currentUser: FirebaseUser
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: EditorViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val localRepo = ProjectRepository(AppDatabase.getInstance(app).projectDao())
                val cloudRepo = FirestoreProjectRepository(firestore, Firebase.auth)
                EditorViewModel(
                    repository = localRepo,
                    firestoreRepository = cloudRepo,
                    currentUserId = currentUser.uid,
                    currentUserDisplayName = currentUser.displayName,
                    currentUserPhotoUrl = currentUser.photoUrl?.toString()
                )
            }
        }
    )

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.LANDING) }

    Crossfade(
        targetState = currentScreen,
        label = "AppScreenTransition",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.LANDING -> {
                LandingScreen(
                    viewModel = viewModel,
                    onOpenProject = {
                        currentScreen = AppScreen.EDITOR
                    },
                    userDisplayName = currentUser.displayName,
                    userEmail = currentUser.email,
                    onSignOutClicked = {
                        signOutGoogle(
                            credentialManager = credentialManager,
                            scope = coroutineScope
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.EDITOR -> {
                EditorScreen(
                    viewModel = viewModel,
                    onNavigateToLanding = {
                        currentScreen = AppScreen.LANDING
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun GuestMotionCutApp(
    onSignInRequested: () -> Unit
) {
    val viewModel: EditorViewModel = viewModel(
        key = "guest_local_session",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val localRepo = ProjectRepository(AppDatabase.getInstance(app).projectDao())
                EditorViewModel(
                    repository = localRepo,
                    firestoreRepository = null,
                    currentUserId = null
                )
            }
        }
    )

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.LANDING) }

    Crossfade(
        targetState = currentScreen,
        label = "GuestScreenTransition",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.LANDING -> {
                LandingScreen(
                    viewModel = viewModel,
                    onOpenProject = {
                        currentScreen = AppScreen.EDITOR
                    },
                    userDisplayName = "Local Studio (Guest)",
                    userEmail = null,
                    onSignOutClicked = null,
                    onSignInClicked = onSignInRequested,
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.EDITOR -> {
                EditorScreen(
                    viewModel = viewModel,
                    onNavigateToLanding = {
                        currentScreen = AppScreen.LANDING
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
