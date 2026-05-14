// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/navigation/AppNavGraph.kt
package edu.nd.pmcburne.hwapp.one.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import edu.nd.pmcburne.hwapp.one.data.FirestoreEntry
import edu.nd.pmcburne.hwapp.one.data.FirestoreRepository
import edu.nd.pmcburne.hwapp.one.screens.CreateEntryScreen
import edu.nd.pmcburne.hwapp.one.screens.DraftsScreen
import edu.nd.pmcburne.hwapp.one.screens.EditEntryScreen
import edu.nd.pmcburne.hwapp.one.screens.EntryDetailScreen
import edu.nd.pmcburne.hwapp.one.screens.HomeScreen
import edu.nd.pmcburne.hwapp.one.screens.LoginScreen
import edu.nd.pmcburne.hwapp.one.screens.ProfileScreen
import edu.nd.pmcburne.hwapp.one.screens.SettingsScreen
import edu.nd.pmcburne.hwapp.one.screens.SignUpScreen
import edu.nd.pmcburne.hwapp.one.ui.theme.BrownText
import edu.nd.pmcburne.hwapp.one.ui.theme.CreamBackground

object AppRoutes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val HOME = "home"
    const val ENTRY_DETAIL = "entryDetail"
    const val PROFILE = "profile"
    const val CREATE_ENTRY = "createEntry"
    const val DRAFTS = "drafts"
    const val EDIT_ENTRY = "editEntry"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(
    onThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    fun logoutToLogin() {
        FirebaseAuth.getInstance().signOut()
        navController.navigate(AppRoutes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LOGIN
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginClick = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(AppRoutes.SIGN_UP)
                }
            )
        }

        composable(AppRoutes.SIGN_UP) {
            SignUpScreen(
                onCreateAccountClick = {
                    navController.navigate(AppRoutes.PROFILE) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoutes.HOME) {
            HomeScreen(
                onEntryClick = { entryId ->
                    navController.navigate("${AppRoutes.ENTRY_DETAIL}/$entryId")
                },
                onProfileClick = {
                    navController.navigate(AppRoutes.PROFILE)
                },
                onCreateClick = {
                    navController.navigate(AppRoutes.CREATE_ENTRY)
                },
                onDraftsClick = {
                    navController.navigate(AppRoutes.DRAFTS)
                },
                onSettingsClick = {
                    navController.navigate(AppRoutes.SETTINGS)
                },
                onLogoutClick = {}
            )
        }

        composable(
            route = "${AppRoutes.ENTRY_DETAIL}/{entryId}",
            arguments = listOf(
                navArgument("entryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId")
            var entry by remember(entryId) { mutableStateOf<FirestoreEntry?>(null) }
            var errorMessage by remember(entryId) { mutableStateOf("") }

            DisposableEffect(entryId) {
                if (entryId == null) {
                    onDispose {}
                } else {
                    val listener = FirestoreRepository.listenToEntry(
                        entryId = entryId,
                        onSuccess = { loadedEntry ->
                            entry = loadedEntry
                            errorMessage = ""
                        },
                        onError = { message ->
                            errorMessage = message
                        }
                    )

                    onDispose {
                        listener.remove()
                    }
                }
            }

            when {
                entryId == null -> EntryStateMessage("Post not found.")
                errorMessage.isNotBlank() -> EntryStateMessage(errorMessage)
                entry == null -> EntryStateMessage("Loading post...")
                else -> {
                    EntryDetailScreen(
                        entry = entry!!,
                        onHomeClick = {
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onProfileClick = {
                            navController.navigate(AppRoutes.PROFILE)
                        },
                        onCreateClick = {
                            navController.navigate(AppRoutes.CREATE_ENTRY)
                        },
                        onDraftsClick = {
                            navController.navigate(AppRoutes.DRAFTS)
                        },
                        onEditClick = { id ->
                            navController.navigate("${AppRoutes.EDIT_ENTRY}/$id")
                        },
                        onDeleteClick = { id ->
                            FirestoreRepository.deleteEntry(
                                entryId = id,
                                onSuccess = {
                                    navController.navigate(AppRoutes.HOME) {
                                        popUpTo(AppRoutes.HOME) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                onError = {
                                    navController.navigate(AppRoutes.HOME) {
                                        popUpTo(AppRoutes.HOME) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        },
                        onSettingsClick = {
                            navController.navigate(AppRoutes.SETTINGS)
                        }
                    )
                }
            }
        }

        composable(AppRoutes.PROFILE) {
            ProfileScreen(
                onHomeClick = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProfileClick = {},
                onCreateClick = {
                    navController.navigate(AppRoutes.CREATE_ENTRY)
                },
                onDraftsClick = {
                    navController.navigate(AppRoutes.DRAFTS)
                },
                onSettingsClick = {
                    navController.navigate(AppRoutes.SETTINGS)
                },
                onLogoutClick = {}
            )
        }

        composable(AppRoutes.CREATE_ENTRY) {
            CreateEntryScreen(
                onHomeClick = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProfileClick = {
                    navController.navigate(AppRoutes.PROFILE)
                },
                onDraftsClick = {
                    navController.navigate(AppRoutes.DRAFTS)
                },
                onSettingsClick = {
                    navController.navigate(AppRoutes.SETTINGS)
                },
                onLogoutClick = {}
            )
        }

        composable(AppRoutes.DRAFTS) {
            DraftsScreen(
                onDraftClick = { draftId ->
                    navController.navigate("${AppRoutes.EDIT_ENTRY}/$draftId")
                },
                onHomeClick = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProfileClick = {
                    navController.navigate(AppRoutes.PROFILE) {
                        popUpTo(AppRoutes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onCreateClick = {
                    navController.navigate(AppRoutes.CREATE_ENTRY)
                },
                onSettingsClick = {
                    navController.navigate(AppRoutes.SETTINGS)
                },
                onLogoutClick = {}
            )
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onHomeClick = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onProfileClick = {
                    navController.navigate(AppRoutes.PROFILE) {
                        launchSingleTop = true
                    }
                },
                onDraftsClick = {
                    navController.navigate(AppRoutes.DRAFTS)
                },
                onCreateClick = {
                    navController.navigate(AppRoutes.CREATE_ENTRY)
                },
                onLogoutConfirmed = {
                    logoutToLogin()
                },
                onThemeChanged = onThemeChanged
            )
        }

        composable(
            route = "${AppRoutes.EDIT_ENTRY}/{entryId}",
            arguments = listOf(
                navArgument("entryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId")
            var entry by remember(entryId) { mutableStateOf<FirestoreEntry?>(null) }
            var errorMessage by remember(entryId) { mutableStateOf("") }

            DisposableEffect(entryId) {
                if (entryId == null) {
                    onDispose {}
                } else {
                    val listener = FirestoreRepository.listenToEntry(
                        entryId = entryId,
                        onSuccess = { loadedEntry ->
                            entry = loadedEntry
                            errorMessage = ""
                        },
                        onError = { message ->
                            errorMessage = message
                        }
                    )

                    onDispose {
                        listener.remove()
                    }
                }
            }

            when {
                entryId == null -> EntryStateMessage("Entry not found.")
                errorMessage.isNotBlank() -> EntryStateMessage(errorMessage)
                entry == null -> EntryStateMessage("Loading entry...")
                else -> {
                    EditEntryScreen(
                        entry = entry!!,
                        onHomeClick = {
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onProfileClick = {
                            navController.navigate(AppRoutes.PROFILE)
                        },
                        onDraftsClick = {
                            navController.navigate(AppRoutes.DRAFTS) {
                                popUpTo(AppRoutes.DRAFTS) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onCancelClick = {
                            navController.popBackStack()
                        },
                        onSettingsClick = {
                            navController.navigate(AppRoutes.SETTINGS)
                        },
                        onLogoutClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = BrownText
        )
    }
}

