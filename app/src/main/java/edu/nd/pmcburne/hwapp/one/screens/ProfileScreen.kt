// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/screens/ProfileScreen.kt
package edu.nd.pmcburne.hwapp.one.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.nd.pmcburne.hwapp.one.data.FirestoreRepository
import edu.nd.pmcburne.hwapp.one.data.StorageRepository

private const val PROFILE_PREFS = "memo_profile_prefs"
private const val KEY_SHOW_PHOTO_MODAL = "show_photo_modal"
private const val KEY_SHOW_LOCATION_MODAL = "show_location_modal"
private const val KEY_LOCATION_GRANTED = "location_granted"
private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"

enum class ProfileBottomTab {
    HOME,
    DRAFTS,
    CREATE,
    PROFILE
}

@Composable
fun ProfileScreen(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceTextColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    val currentUser = auth.currentUser
    val userId = currentUser?.uid.orEmpty()
    val authEmail = currentUser?.email.orEmpty()
    val defaultUsername = authEmail.substringBefore("@").ifBlank { "username" }
    val defaultFullName = currentUser?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: defaultUsername.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }

    fun userKey(base: String): String = "${userId}_$base"

    var fullName by rememberSaveable(userId) { mutableStateOf(defaultFullName) }
    var username by rememberSaveable(userId) { mutableStateOf(defaultUsername) }
    var profilePhotoUrl by rememberSaveable(userId) { mutableStateOf("") }
    var localCameraPhotoBase64 by rememberSaveable(userId) { mutableStateOf("") }

    var showLocationModal by rememberSaveable(userId) {
        mutableStateOf(
            if (userId.isBlank()) false
            else prefs.getBoolean(userKey(KEY_SHOW_LOCATION_MODAL), false)
        )
    }
    var showPhotoSourceDialog by rememberSaveable(userId) {
        mutableStateOf(
            if (userId.isBlank()) false
            else prefs.getBoolean(userKey(KEY_SHOW_PHOTO_MODAL), false)
        )
    }
    var locationGranted by rememberSaveable(userId) {
        mutableStateOf(
            if (userId.isBlank()) {
                hasLocationPermission(context)
            } else {
                prefs.getBoolean(userKey(KEY_LOCATION_GRANTED), false) || hasLocationPermission(context)
            }
        )
    }
    var permissionMessage by rememberSaveable(userId) { mutableStateOf("") }
    var tripCount by remember(userId) { mutableIntStateOf(0) }
    var isUploadingPhoto by rememberSaveable(userId) { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect

        val onboardingShown = prefs.getBoolean(userKey(KEY_ONBOARDING_SHOWN), false)

        if (!onboardingShown) {
            showPhotoSourceDialog = true
            showLocationModal = true

            prefs.edit()
                .putBoolean(userKey(KEY_SHOW_PHOTO_MODAL), true)
                .putBoolean(userKey(KEY_SHOW_LOCATION_MODAL), true)
                .putBoolean(userKey(KEY_ONBOARDING_SHOWN), true)
                .apply()
        }
    }

    DisposableEffect(userId) {
        if (userId.isBlank()) {
            tripCount = 0
            onDispose {}
        } else {
            val listener = db.collection("entries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("public", true)
                .addSnapshotListener { snapshot, _ ->
                    tripCount = snapshot?.documents?.size ?: 0
                }

            onDispose {
                listener.remove()
            }
        }
    }

    DisposableEffect(userId) {
        if (userId.isBlank()) {
            onDispose {}
        } else {
            val listener = FirestoreRepository.listenToUserProfile(
                userId = userId,
                onSuccess = { profile ->
                    if (profile != null) {
                        if (profile.fullName.isNotBlank()) fullName = profile.fullName
                        if (profile.username.isNotBlank()) username = profile.username
                        if (profile.photoUrl.isNotBlank()) {
                            profilePhotoUrl = profile.photoUrl
                            localCameraPhotoBase64 = ""
                        }
                    }
                },
                onError = { message ->
                    permissionMessage = message
                }
            )

            onDispose {
                listener.remove()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        locationGranted = granted
        if (userId.isNotBlank()) {
            prefs.edit()
                .putBoolean(userKey(KEY_LOCATION_GRANTED), granted)
                .putBoolean(userKey(KEY_SHOW_LOCATION_MODAL), false)
                .apply()
        }

        showLocationModal = false
        permissionMessage = if (granted) "" else "Location sharing is off. You can turn it on later."
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && userId.isNotBlank()) {
            isUploadingPhoto = true
            permissionMessage = ""

            StorageRepository.uploadProfilePhoto(
                userId = userId,
                fileUri = uri,
                onSuccess = { downloadUrl ->
                    profilePhotoUrl = downloadUrl
                    localCameraPhotoBase64 = ""

                    FirestoreRepository.updateUserPhotoUrl(
                        userId = userId,
                        photoUrl = downloadUrl,
                        onSuccess = {
                            isUploadingPhoto = false
                            prefs.edit()
                                .putBoolean(userKey(KEY_SHOW_PHOTO_MODAL), false)
                                .apply()
                            showPhotoSourceDialog = false
                        },
                        onError = { message ->
                            isUploadingPhoto = false
                            permissionMessage = message
                        }
                    )
                },
                onError = { message ->
                    isUploadingPhoto = false
                    permissionMessage = message
                }
            )
        }
    }

    val takePhotoPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            localCameraPhotoBase64 = encodeBitmapToBase64(bitmap)
            profilePhotoUrl = ""
            if (userId.isNotBlank()) {
                prefs.edit()
                    .putBoolean(userKey(KEY_SHOW_PHOTO_MODAL), false)
                    .apply()
            }
            showPhotoSourceDialog = false
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePhotoPreviewLauncher.launch(null)
        } else {
            permissionMessage = "Camera access was denied."
        }
    }

    if (showLocationModal) {
        AlertDialog(
            onDismissRequest = {
                showLocationModal = false
                if (userId.isNotBlank()) {
                    prefs.edit()
                        .putBoolean(userKey(KEY_SHOW_LOCATION_MODAL), false)
                        .apply()
                }
            },
            containerColor = surfaceColor,
            title = {
                Text(
                    text = "Share your location?",
                    color = surfaceTextColor
                )
            },
            text = {
                Text(
                    text = "Allow location so your trip map can be used in the app.",
                    color = surfaceTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text(
                        text = "Allow",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        locationGranted = false
                        showLocationModal = false
                        if (userId.isNotBlank()) {
                            prefs.edit()
                                .putBoolean(userKey(KEY_LOCATION_GRANTED), false)
                                .putBoolean(userKey(KEY_SHOW_LOCATION_MODAL), false)
                                .apply()
                        }
                    }
                ) {
                    Text(
                        text = "Not now",
                        color = surfaceTextColor
                    )
                }
            }
        )
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = {
                showPhotoSourceDialog = false
                if (userId.isNotBlank()) {
                    prefs.edit()
                        .putBoolean(userKey(KEY_SHOW_PHOTO_MODAL), false)
                        .apply()
                }
            },
            containerColor = surfaceColor,
            title = {
                Text(
                    text = "Add a profile photo",
                    color = surfaceTextColor
                )
            },
            text = {
                Text(
                    text = if (isUploadingPhoto) {
                        "Uploading profile photo..."
                    } else {
                        "Choose how you'd like to set your profile picture."
                    },
                    color = surfaceTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isUploadingPhoto) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Text(
                        text = "Camera",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            if (!isUploadingPhoto) {
                                pickPhotoLauncher.launch("image/*")
                            }
                        }
                    ) {
                        Text(
                            text = "Photo Library",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(
                        onClick = {
                            if (!isUploadingPhoto) {
                                showPhotoSourceDialog = false
                                if (userId.isNotBlank()) {
                                    prefs.edit()
                                        .putBoolean(userKey(KEY_SHOW_PHOTO_MODAL), false)
                                        .apply()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = "Later",
                            color = surfaceTextColor
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            ProfileBottomBar(
                selectedTab = ProfileBottomTab.PROFILE,
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
            ProfileTopBar(
                onSettingsClick = onSettingsClick
            )

            HorizontalDivider(color = borderColor)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(surfaceColor)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable {
                            showPhotoSourceDialog = true
                            if (userId.isNotBlank()) {
                                prefs.edit()
                                    .putBoolean(userKey(KEY_SHOW_PHOTO_MODAL), true)
                                    .apply()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        profilePhotoUrl.isNotBlank() -> {
                            AsyncImage(
                                model = profilePhotoUrl,
                                contentDescription = "Profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                            )
                        }

                        localCameraPhotoBase64.isNotBlank() -> {
                            val bitmap = decodeBase64ToBitmap(localCameraPhotoBase64)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile avatar",
                                    tint = surfaceTextColor,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile avatar",
                                tint = surfaceTextColor,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Text(
                    text = if (username.isBlank()) defaultUsername else username,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.size(6.dp))

                Text(
                    text = if (isUploadingPhoto) "Uploading profile photo..." else "Edit profile photo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.clickable {
                        if (!isUploadingPhoto) {
                            showPhotoSourceDialog = true
                            if (userId.isNotBlank()) {
                                prefs.edit()
                                    .putBoolean(userKey(KEY_SHOW_PHOTO_MODAL), true)
                                    .apply()
                            }
                        }
                    }
                )

                if (permissionMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = permissionMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.size(20.dp))

                Text(
                    text = "Number of Trips : $tripCount",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.size(20.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (userId.isNotBlank()) {
                            FirestoreRepository.updateUserProfileFields(
                                userId = userId,
                                fullName = it,
                                username = username,
                                onSuccess = {},
                                onError = { message -> permissionMessage = message }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = surfaceTextColor,
                        unfocusedTextColor = surfaceTextColor,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = surfaceTextColor.copy(alpha = 0.7f),
                        cursorColor = surfaceTextColor
                    )
                )

                Spacer(modifier = Modifier.size(14.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (userId.isNotBlank()) {
                            FirestoreRepository.updateUserProfileFields(
                                userId = userId,
                                fullName = fullName,
                                username = it,
                                onSuccess = {},
                                onError = { message -> permissionMessage = message }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = surfaceTextColor,
                        unfocusedTextColor = surfaceTextColor,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = surfaceTextColor.copy(alpha = 0.7f),
                        cursorColor = surfaceTextColor
                    )
                )
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    onSettingsClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    var showMenu by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = textColor,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = textColor,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { showMenu = true }
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = surfaceColor
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Settings",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        showMenu = false
                        onSettingsClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileBottomBar(
    selectedTab: ProfileBottomTab,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onDraftsClick: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val iconColor = MaterialTheme.colorScheme.onSurface
    var showCreateMenu by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor)
            .background(surfaceColor)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileBottomBarIcon(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            selected = selectedTab == ProfileBottomTab.HOME,
            onClick = onHomeClick
        )

        ProfileBottomBarIcon(
            icon = {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Drafts",
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            },
            selected = selectedTab == ProfileBottomTab.DRAFTS,
            onClick = onDraftsClick
        )

        Box {
            ProfileBottomBarIcon(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, iconColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create",
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                selected = selectedTab == ProfileBottomTab.CREATE,
                onClick = { showCreateMenu = true }
            )

            DropdownMenu(
                expanded = showCreateMenu,
                onDismissRequest = { showCreateMenu = false },
                containerColor = surfaceColor
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Go to drafts",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        showCreateMenu = false
                        onDraftsClick()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Create new entry",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        showCreateMenu = false
                        onCreateClick()
                    }
                )
            }
        }

        ProfileBottomBarIcon(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            selected = selectedTab == ProfileBottomTab.PROFILE,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun ProfileBottomBarIcon(
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                color = if (selected) selectedBackground else androidx.compose.ui.graphics.Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

private fun encodeBitmapToBase64(bitmap: Bitmap): String {
    val outputStream = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}

private fun decodeBase64ToBitmap(encoded: String): Bitmap? {
    return runCatching {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}