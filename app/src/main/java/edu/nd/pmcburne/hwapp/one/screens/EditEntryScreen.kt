// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/screens/EditEntryScreen.kt
package edu.nd.pmcburne.hwapp.one.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import edu.nd.pmcburne.hwapp.one.data.FirestoreEntry
import edu.nd.pmcburne.hwapp.one.data.FirestoreRepository
import edu.nd.pmcburne.hwapp.one.data.StorageRepository
import java.io.File

private const val EDIT_ENTRY_PREFS = "memo_edit_entry_prefs"
private const val KEY_SHOW_EDIT_CAMERA_MODAL = "show_edit_camera_modal"

@Composable
fun EditEntryScreen(
    entry: FirestoreEntry,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val cameraPrefs = remember {
        context.getSharedPreferences(EDIT_ENTRY_PREFS, Context.MODE_PRIVATE)
    }

    var title by rememberSaveable(entry.id) { mutableStateOf(entry.title) }
    var location by rememberSaveable(entry.id) { mutableStateOf(entry.location) }
    var date by rememberSaveable(entry.id) { mutableStateOf(entry.date) }
    var content by rememberSaveable(entry.id) { mutableStateOf(entry.previewText) }
    var tags by rememberSaveable(entry.id) { mutableStateOf(entry.tags.joinToString(", ")) }
    var errorMessage by rememberSaveable(entry.id) { mutableStateOf("") }
    var isSaving by rememberSaveable(entry.id) { mutableStateOf(false) }

    var selectedImageUri by rememberSaveable(entry.id) { mutableStateOf("") }
    var showPhotoSourceMenu by rememberSaveable(entry.id) { mutableStateOf(false) }
    var showCameraPermissionDialog by rememberSaveable(entry.id) {
        mutableStateOf(cameraPrefs.getBoolean(KEY_SHOW_EDIT_CAMERA_MODAL, false))
    }
    var pendingCameraLaunch by rememberSaveable(entry.id) { mutableStateOf(false) }
    var cameraImageUriString by rememberSaveable(entry.id) { mutableStateOf("") }

    val isPublicEntry = entry.public

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceTextColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val accentColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    val selectedBitmap = remember(selectedImageUri) {
        if (selectedImageUri.isBlank()) {
            null
        } else {
            runCatching {
                context.contentResolver
                    .openInputStream(Uri.parse(selectedImageUri))
                    ?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
            errorMessage = ""
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUriString.isNotBlank()) {
            selectedImageUri = cameraImageUriString
            errorMessage = ""
        } else {
            cameraImageUriString = ""
        }
        pendingCameraLaunch = false
    }

    val launchCameraCapture: () -> Unit = {
        val tempUri = createEditTempImageUri(context)
        if (tempUri == null) {
            errorMessage = "Unable to open camera right now."
            pendingCameraLaunch = false
        } else {
            cameraImageUriString = tempUri.toString()
            cameraLauncher.launch(tempUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPrefs.edit()
            .putBoolean(KEY_SHOW_EDIT_CAMERA_MODAL, false)
            .apply()

        showCameraPermissionDialog = false

        if (granted) {
            launchCameraCapture()
        } else {
            pendingCameraLaunch = false
            errorMessage = "Camera permission denied."
        }
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showCameraPermissionDialog = false
                pendingCameraLaunch = false
                cameraPrefs.edit()
                    .putBoolean(KEY_SHOW_EDIT_CAMERA_MODAL, false)
                    .apply()
            },
            containerColor = surfaceColor,
            title = {
                Text(
                    text = "Allow camera access?",
                    color = surfaceTextColor
                )
            },
            text = {
                Text(
                    text = "Enable camera access so you can take a photo for your entry.",
                    color = surfaceTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text(
                        text = "Allow",
                        color = accentColor
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCameraPermissionDialog = false
                        pendingCameraLaunch = false
                        cameraPrefs.edit()
                            .putBoolean(KEY_SHOW_EDIT_CAMERA_MODAL, false)
                            .apply()
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

    fun validate(): Boolean {
        return when {
            title.trim().isBlank() ||
                    location.trim().isBlank() ||
                    date.trim().isBlank() ||
                    content.trim().isBlank() -> {
                errorMessage = "Please fill in all fields."
                false
            }

            auth.currentUser?.uid.isNullOrBlank() -> {
                errorMessage = "You must be logged in to update this entry."
                false
            }

            else -> {
                errorMessage = ""
                true
            }
        }
    }

    fun buildUpdatedEntry(makePublic: Boolean, photoUrl: String): FirestoreEntry {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        val currentUsername = auth.currentUser?.email
            ?.substringBefore("@")
            ?.ifBlank { entry.username }
            ?: entry.username

        return FirestoreEntry(
            id = entry.id,
            userId = if (currentUserId.isBlank()) entry.userId else currentUserId,
            username = currentUsername,
            title = title.trim(),
            location = location.trim(),
            date = date.trim(),
            previewText = content.trim(),
            tags = tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() },
            public = makePublic,
            createdAt = entry.createdAt,
            photoUrl = photoUrl
        )
    }

    fun completeSave(makePublic: Boolean, photoUrl: String) {
        FirestoreRepository.updateEntry(
            entry = buildUpdatedEntry(makePublic = makePublic, photoUrl = photoUrl),
            onSuccess = {
                isSaving = false
                if (makePublic) onHomeClick() else onDraftsClick()
            },
            onError = { message ->
                isSaving = false
                errorMessage = message
            }
        )
    }

    fun saveEntry(makePublic: Boolean) {
        if (!validate()) return

        val userId = auth.currentUser?.uid.orEmpty()
        if (userId.isBlank()) {
            errorMessage = "You must be logged in to update this entry."
            return
        }

        isSaving = true

        if (selectedImageUri.isBlank()) {
            completeSave(makePublic = makePublic, photoUrl = entry.photoUrl)
            return
        }

        StorageRepository.uploadEntryPhoto(
            userId = userId,
            entryId = entry.id,
            fileUri = Uri.parse(selectedImageUri),
            onSuccess = { downloadUrl ->
                completeSave(makePublic = makePublic, photoUrl = downloadUrl)
            },
            onError = { message ->
                isSaving = false
                errorMessage = message
            }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            CreateEntryBottomBar(
                onHomeClick = onHomeClick,
                onProfileClick = onProfileClick,
                onDraftsClick = onDraftsClick,
                onCreateClick = {}
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
                title = "Edit Entry",
                onSettingsClick = onSettingsClick
            )
            HorizontalDivider(color = borderColor)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CreateEntryField(
                    label = "Title",
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = ""
                    },
                    placeholder = "Trip title"
                )

                EntryLocationPickerField(
                    label = "Location",
                    value = location,
                    onLocationSelected = {
                        location = it
                        errorMessage = ""
                    }
                )

                EntryDatePickerField(
                    label = "Date",
                    value = date,
                    onDateSelected = {
                        date = it
                        errorMessage = ""
                    }
                )

                CreateEntryField(
                    label = "Tags",
                    value = tags,
                    onValueChange = {
                        tags = it
                        errorMessage = ""
                    },
                    placeholder = "food, beach, friends"
                )

                Text(
                    text = "Photo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                            .background(surfaceColor, RoundedCornerShape(18.dp))
                            .clickable { showPhotoSourceMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            selectedBitmap != null -> {
                                Image(
                                    bitmap = selectedBitmap.asImageBitmap(),
                                    contentDescription = "Selected entry photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(18.dp))
                                )
                            }

                            entry.photoUrl.isNotBlank() -> {
                                AsyncImage(
                                    model = entry.photoUrl,
                                    contentDescription = "Current entry photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(18.dp))
                                )
                            }

                            else -> {
                                Text(
                                    text = "Tap to add a picture",
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showPhotoSourceMenu,
                        onDismissRequest = { showPhotoSourceMenu = false },
                        containerColor = surfaceColor
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Choose from photos",
                                    color = surfaceTextColor
                                )
                            },
                            onClick = {
                                showPhotoSourceMenu = false
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Take photo",
                                    color = surfaceTextColor
                                )
                            },
                            onClick = {
                                showPhotoSourceMenu = false
                                if (hasEditCameraPermission(context)) {
                                    launchCameraCapture()
                                } else {
                                    pendingCameraLaunch = true
                                    cameraPrefs.edit()
                                        .putBoolean(KEY_SHOW_EDIT_CAMERA_MODAL, true)
                                        .apply()
                                    showCameraPermissionDialog = true
                                }
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        errorMessage = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = {
                        Text(
                            text = "Write your memory...",
                            color = surfaceTextColor.copy(alpha = 0.6f)
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = surfaceTextColor,
                        unfocusedTextColor = surfaceTextColor,
                        cursorColor = surfaceTextColor
                    )
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = errorColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCancelClick,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = surfaceColor,
                            contentColor = textColor
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { saveEntry(makePublic = isPublicEntry) },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            when {
                                isSaving -> "Updating..."
                                isPublicEntry -> "Update Entry"
                                else -> "Update Draft"
                            }
                        )
                    }
                }

                if (isPublicEntry) {
                    Button(
                        onClick = { saveEntry(makePublic = false) },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = surfaceColor,
                            contentColor = textColor
                        )
                    ) {
                        Text("Move to Drafts")
                    }
                } else {
                    Button(
                        onClick = { saveEntry(makePublic = true) },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = surfaceColor,
                            contentColor = textColor
                        )
                    ) {
                        Text("Publish to Home")
                    }
                }

                TextButton(
                    onClick = {
                        if (isPublicEntry) onHomeClick() else onDraftsClick()
                    },
                    enabled = !isSaving
                ) {
                    Text(
                        text = if (isPublicEntry) "Back to Home" else "Back to Drafts",
                        color = textColor
                    )
                }
            }
        }
    }
}

private fun hasEditCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun createEditTempImageUri(context: Context): Uri? {
    return runCatching {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File.createTempFile("memo_edit_camera_", ".jpg", imagesDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }.getOrNull()
}