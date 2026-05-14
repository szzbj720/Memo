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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import edu.nd.pmcburne.hwapp.one.data.FirestoreEntry
import edu.nd.pmcburne.hwapp.one.data.FirestoreRepository
import edu.nd.pmcburne.hwapp.one.data.StorageRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val CREATE_ENTRY_PREFS = "memo_create_entry_prefs"
private const val KEY_SHOW_CAMERA_MODAL = "show_camera_modal"

@Composable
fun CreateEntryScreen(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    val cameraPrefs = remember {
        context.getSharedPreferences(CREATE_ENTRY_PREFS, Context.MODE_PRIVATE)
    }

    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isPosting by rememberSaveable { mutableStateOf(false) }
    var isSavingDraft by rememberSaveable { mutableStateOf(false) }
    var showPhotoSourceMenu by rememberSaveable { mutableStateOf(false) }
    var showCameraPermissionDialog by rememberSaveable {
        mutableStateOf(cameraPrefs.getBoolean(KEY_SHOW_CAMERA_MODAL, false))
    }
    var pendingCameraLaunch by rememberSaveable { mutableStateOf(false) }
    var cameraImageUriString by rememberSaveable { mutableStateOf("") }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceTextColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val accentColor = MaterialTheme.colorScheme.primary

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
        val tempUri = createTempImageUri(context)
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
            .putBoolean(KEY_SHOW_CAMERA_MODAL, false)
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
                    .putBoolean(KEY_SHOW_CAMERA_MODAL, false)
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
                    text = "Enable camera access so you can take a photo for your post.",
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
                            .putBoolean(KEY_SHOW_CAMERA_MODAL, false)
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

    fun finishSubmit(makePublic: Boolean, photoUrl: String) {
        val trimmedTitle = title.trim()
        val trimmedLocation = location.trim()
        val trimmedDate = date.trim()
        val trimmedContent = content.trim()
        val parsedTags = tags.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val currentUser = auth.currentUser
        val userId = currentUser?.uid.orEmpty()
        val username = currentUser?.email
            ?.substringBefore("@")
            ?.ifBlank { "anonymous" }
            ?: "anonymous"

        val entryId = UUID.randomUUID().toString()

        FirestoreRepository.createEntry(
            entry = FirestoreEntry(
                id = entryId,
                userId = userId,
                username = username,
                title = trimmedTitle,
                location = trimmedLocation,
                date = trimmedDate,
                previewText = trimmedContent,
                tags = parsedTags,
                public = makePublic,
                createdAt = System.currentTimeMillis(),
                photoUrl = photoUrl
            ),
            onSuccess = {
                isPosting = false
                isSavingDraft = false
                if (makePublic) onHomeClick() else onDraftsClick()
            },
            onError = { message ->
                isPosting = false
                isSavingDraft = false
                errorMessage = message
            }
        )
    }

    fun submitEntry(makePublic: Boolean) {
        val trimmedTitle = title.trim()
        val trimmedLocation = location.trim()
        val trimmedDate = date.trim()
        val trimmedContent = content.trim()

        val currentUser = auth.currentUser
        val userId = currentUser?.uid.orEmpty()

        when {
            trimmedTitle.isBlank() ||
                    trimmedLocation.isBlank() ||
                    trimmedDate.isBlank() ||
                    trimmedContent.isBlank() -> {
                errorMessage = "Please fill in all fields."
                return
            }

            userId.isBlank() -> {
                errorMessage = if (makePublic) {
                    "You must be logged in to post an entry."
                } else {
                    "You must be logged in to save a draft."
                }
                return
            }
        }

        errorMessage = ""
        if (makePublic) isPosting = true else isSavingDraft = true

        if (selectedImageUri.isBlank()) {
            finishSubmit(makePublic = makePublic, photoUrl = "")
            return
        }

        val entryId = UUID.randomUUID().toString()

        StorageRepository.uploadEntryPhoto(
            userId = userId,
            entryId = entryId,
            fileUri = Uri.parse(selectedImageUri),
            onSuccess = { downloadUrl ->
                val parsedTags = tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val username = currentUser?.email
                    ?.substringBefore("@")
                    ?.ifBlank { "anonymous" }
                    ?: "anonymous"

                FirestoreRepository.createEntry(
                    entry = FirestoreEntry(
                        id = entryId,
                        userId = userId,
                        username = username,
                        title = trimmedTitle,
                        location = trimmedLocation,
                        date = trimmedDate,
                        previewText = trimmedContent,
                        tags = parsedTags,
                        public = makePublic,
                        createdAt = System.currentTimeMillis(),
                        photoUrl = downloadUrl
                    ),
                    onSuccess = {
                        isPosting = false
                        isSavingDraft = false
                        if (makePublic) onHomeClick() else onDraftsClick()
                    },
                    onError = { message ->
                        isPosting = false
                        isSavingDraft = false
                        errorMessage = message
                    }
                )
            },
            onError = { message ->
                isPosting = false
                isSavingDraft = false
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
                title = "Create Entry",
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
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap.asImageBitmap(),
                                contentDescription = "Selected entry photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        } else {
                            Text(
                                text = "Tap to add a picture",
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                                if (hasCameraPermission(context)) {
                                    launchCameraCapture()
                                } else {
                                    pendingCameraLaunch = true
                                    cameraPrefs.edit()
                                        .putBoolean(KEY_SHOW_CAMERA_MODAL, true)
                                        .apply()
                                    showCameraPermissionDialog = true
                                }
                            }
                        )
                    }
                }

                Text(
                    text = "Entry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

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

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { submitEntry(makePublic = true) },
                        enabled = !isPosting && !isSavingDraft,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(if (isPosting) "Posting..." else "Post Entry")
                    }

                    Button(
                        onClick = { submitEntry(makePublic = false) },
                        enabled = !isPosting && !isSavingDraft,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(if (isSavingDraft) "Saving..." else "Save as Draft")
                    }
                }
            }
        }
    }
}

private fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun createTempImageUri(context: Context): Uri? {
    return runCatching {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File.createTempFile("memo_camera_", ".jpg", imagesDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }.getOrNull()
}

@Composable
fun CreateEntryField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceTextColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
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
    }
}

@Composable
fun CreateEntryBottomBar(
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    var showCreateMenu by rememberSaveable { mutableStateOf(false) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val iconColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor)
            .background(surfaceColor)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CreateEntryBottomBarIcon(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            selected = false,
            onClick = onHomeClick
        )

        CreateEntryBottomBarIcon(
            icon = {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Drafts",
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            },
            selected = false,
            onClick = onDraftsClick
        )

        Box {
            CreateEntryBottomBarIcon(
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
                selected = false,
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

        CreateEntryBottomBarIcon(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            selected = false,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun CreateEntryBottomBarIcon(
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                color = if (selected) selectedBackground else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

val presetEntryLocations = listOf(
    "Chicago, Illinois",
    "Aspen, Colorado",
    "Miami, Florida",
    "New York, New York",
    "Los Angeles, California",
    "Boston, Massachusetts",
    "Seattle, Washington",
    "Nashville, Tennessee"
)

private fun formatEntryDate(millis: Long): String {
    return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(millis))
}

@Composable
fun EntryLocationPickerField(
    label: String,
    value: String,
    onLocationSelected: (String) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceTextColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Select a location",
                        color = surfaceTextColor.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Choose location",
                            tint = surfaceTextColor
                        )
                    }
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

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = surfaceColor
            ) {
                presetEntryLocations.forEach { presetLocation ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = presetLocation,
                                color = surfaceTextColor
                            )
                        },
                        onClick = {
                            expanded = false
                            onLocationSelected(presetLocation)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EntryDatePickerField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceTextColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            readOnly = true,
            singleLine = true,
            placeholder = {
                Text(
                    text = "Select a date",
                    color = surfaceTextColor.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Choose date",
                        tint = surfaceTextColor
                    )
                }
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
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(
                containerColor = surfaceColor,
                titleContentColor = textColor,
                headlineContentColor = surfaceTextColor,
                weekdayContentColor = surfaceTextColor,
                subheadContentColor = surfaceTextColor,
                yearContentColor = surfaceTextColor,
                currentYearContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                dayContentColor = surfaceTextColor,
                disabledDayContentColor = surfaceTextColor.copy(alpha = 0.38f),
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                disabledSelectedDayContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                disabledSelectedDayContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            onDateSelected(formatEntryDate(selectedMillis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "OK",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = textColor
                    )
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor,
                    headlineContentColor = surfaceTextColor,
                    weekdayContentColor = surfaceTextColor,
                    subheadContentColor = surfaceTextColor,
                    yearContentColor = surfaceTextColor,
                    currentYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    dayContentColor = surfaceTextColor,
                    disabledDayContentColor = surfaceTextColor.copy(alpha = 0.38f),
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledSelectedDayContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    disabledSelectedDayContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}