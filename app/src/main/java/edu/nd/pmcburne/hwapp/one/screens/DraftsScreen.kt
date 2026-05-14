// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/screens/DraftsScreen.kt
package edu.nd.pmcburne.hwapp.one.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import edu.nd.pmcburne.hwapp.one.data.FirestoreRepository

@Composable
fun DraftsScreen(
    onDraftClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val userId = auth.currentUser?.uid.orEmpty()

    val draftEntries = remember { mutableStateListOf<HomeEntry>() }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(true) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    DisposableEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            errorMessage = "You must be logged in to view drafts."
            onDispose {}
        } else {
            val listener = FirestoreRepository.listenToUserDraftEntries(
                userId = userId,
                onSuccess = { entries ->
                    draftEntries.clear()
                    draftEntries.addAll(entries)
                    isLoading = false
                    errorMessage = ""
                },
                onError = { message ->
                    isLoading = false
                    errorMessage = message
                }
            )

            onDispose {
                listener.remove()
            }
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            CreateEntryBottomBar(
                onHomeClick = onHomeClick,
                onProfileClick = onProfileClick,
                onDraftsClick = {},
                onCreateClick = onCreateClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            HomeTopBar(
                title = "Drafts",
                onSettingsClick = onSettingsClick
            )

            HorizontalDivider(color = borderColor)

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    isLoading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Loading drafts...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor
                                )
                            }
                        }
                    }

                    draftEntries.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No drafts yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor
                                )
                            }
                        }
                    }

                    else -> {
                        items(draftEntries, key = { it.id }) { entry ->
                            FeedEntryCard(
                                entry = entry,
                                onClick = { onDraftClick(entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}