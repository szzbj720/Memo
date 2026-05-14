// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/screens/SettingsScreen.kt
package edu.nd.pmcburne.hwapp.one.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

private const val APP_SETTINGS_PREFS = "memo_app_settings"
private const val KEY_DARK_MODE = "dark_mode"
private const val PROFILE_PREFS = "memo_profile_prefs"
private const val KEY_LOCATION_SHARING_ENABLED = "location_sharing_enabled"

@Composable
fun SettingsScreen(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onCreateClick: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    onThemeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appPrefs = remember {
        context.getSharedPreferences(APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
    }
    val profilePrefs = remember {
        context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid.orEmpty()
    val userEmail = currentUser?.email ?: "No email found"

    fun userKey(base: String): String = "${userId}_$base"

    var darkModeEnabled by rememberSaveable {
        mutableStateOf(appPrefs.getBoolean(KEY_DARK_MODE, false))
    }
    var locationSharingEnabled by rememberSaveable(userId) {
        mutableStateOf(
            if (userId.isBlank()) false
            else profilePrefs.getBoolean(userKey(KEY_LOCATION_SHARING_ENABLED), false)
        )
    }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceTextColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            SettingsBottomBar(
                onHomeClick = onHomeClick,
                onProfileClick = onProfileClick,
                onCreateClick = onCreateClick,
                onDraftsClick = onDraftsClick
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
                title = "Settings",
                onSettingsClick = {}
            )

            HorizontalDivider(color = borderColor)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )

                SettingsInfoCard(
                    title = "Email",
                    subtitle = userEmail
                )

                SettingsToggleCard(
                    title = "Dark Mode",
                    subtitle = if (darkModeEnabled) {
                        "Dark mode is on."
                    } else {
                        "Light mode is on."
                    },
                    checked = darkModeEnabled,
                    onCheckedChange = { enabled ->
                        darkModeEnabled = enabled
                        appPrefs.edit()
                            .putBoolean(KEY_DARK_MODE, enabled)
                            .apply()
                        onThemeChanged(enabled)
                    }
                )

                SettingsToggleCard(
                    title = "Location Sharing",
                    subtitle = if (locationSharingEnabled) {
                        "Map access is on for this account."
                    } else {
                        "Map access is off for this account."
                    },
                    checked = locationSharingEnabled,
                    onCheckedChange = { enabled ->
                        locationSharingEnabled = enabled
                        if (userId.isNotBlank()) {
                            profilePrefs.edit()
                                .putBoolean(userKey(KEY_LOCATION_SHARING_ENABLED), enabled)
                                .apply()
                        }
                    }
                )

                LogoutCard(
                    onClick = { showLogoutDialog = true }
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = surfaceColor,
            title = {
                Text(
                    text = "Log out?",
                    color = surfaceTextColor
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out of your account?",
                    color = surfaceTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutConfirmed()
                    }
                ) {
                    Text(
                        text = "Logout",
                        color = accentColor
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = surfaceTextColor
                    )
                }
            }
        )
    }
}

@Composable
private fun SettingsInfoCard(
    title: String,
    subtitle: String
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .background(surfaceColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = textColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .background(surfaceColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = textColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun LogoutCard(
    onClick: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .background(surfaceColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Text(
            text = "Logout",
            style = MaterialTheme.typography.titleLarge,
            color = accentColor
        )
    }
}

@Composable
private fun SettingsBottomBar(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onDraftsClick: () -> Unit
) {
    var showCreateMenu by rememberSaveable { mutableStateOf(false) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor)
            .background(surfaceColor)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsBottomBarIcon(
            selected = false,
            onClick = onHomeClick
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
        }

        SettingsBottomBarIcon(
            selected = false,
            onClick = onDraftsClick
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = "Drafts",
                tint = textColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Box {
            SettingsBottomBarIcon(
                selected = false,
                onClick = { showCreateMenu = true }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, textColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        tint = textColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showCreateMenu,
                onDismissRequest = { showCreateMenu = false },
                containerColor = surfaceColor
            ) {
                DropdownMenuItem(
                    text = { Text("Go to drafts", color = textColor) },
                    onClick = {
                        showCreateMenu = false
                        onDraftsClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Create new entry", color = textColor) },
                    onClick = {
                        showCreateMenu = false
                        onCreateClick()
                    }
                )
            }
        }

        SettingsBottomBarIcon(
            selected = false,
            onClick = onProfileClick
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SettingsBottomBarIcon(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)

    Box(
        modifier = Modifier
            .size(46.dp)
            .background(
                color = if (selected) selectedColor else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}