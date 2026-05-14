// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/screens/EntryDetailScreen.kt
package edu.nd.pmcburne.hwapp.one.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import edu.nd.pmcburne.hwapp.one.data.FirestoreEntry
import edu.nd.pmcburne.hwapp.one.data.FirestoreRepository
import edu.nd.pmcburne.hwapp.one.ui.theme.BrownText
import edu.nd.pmcburne.hwapp.one.ui.theme.CreamBackground
import edu.nd.pmcburne.hwapp.one.ui.theme.FieldBorder
import edu.nd.pmcburne.hwapp.one.ui.theme.SoftCream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val DETAIL_PROFILE_PREFS = "memo_profile_prefs"
private const val DETAIL_KEY_LOCATION_SHARING_ENABLED = "location_sharing_enabled"

@Composable
fun EntryDetailScreen(
    entry: FirestoreEntry,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid.orEmpty()
    val currentUsername = auth.currentUser?.email
        ?.substringBefore("@")
        ?.ifBlank { "anonymous" }
        ?: "anonymous"
    val isOwner = entry.userId == currentUserId

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(DETAIL_PROFILE_PREFS, Context.MODE_PRIVATE)
    }

    fun userKey(base: String): String = "${currentUserId}_$base"

    var locationSharingEnabled by rememberSaveable(currentUserId) {
        mutableStateOf(
            currentUserId.isNotBlank() &&
                    prefs.getBoolean(userKey(DETAIL_KEY_LOCATION_SHARING_ENABLED), false)
        )
    }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var likeError by rememberSaveable { mutableStateOf("") }
    var isLiking by rememberSaveable { mutableStateOf(false) }
    var isLiked by rememberSaveable(entry.id) { mutableStateOf(false) }
    var likeCount by remember(entry.id) { mutableIntStateOf(0) }

    val latLng = remember(entry.location) { locationToLatLng(entry.location) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLng, 11f)
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    DisposableEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            locationSharingEnabled =
                prefs.getBoolean(userKey(DETAIL_KEY_LOCATION_SHARING_ENABLED), false)
        } else {
            locationSharingEnabled = false
        }
        onDispose {}
    }

    DisposableEffect(entry.id, currentUserId) {
        val listener = FirestoreRepository.listenToLikes(
            entryId = entry.id,
            currentUserId = currentUserId,
            onSuccess = { count, liked ->
                likeCount = count
                isLiked = liked
                likeError = ""
            },
            onError = { message ->
                likeError = message
            }
        )

        onDispose {
            listener.remove()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SoftCream,
            title = { Text("Delete post?", color = BrownText) },
            text = {
                Text(
                    text = "Are you sure you want to delete this post? This action cannot be undone.",
                    color = BrownText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick(entry.id)
                    }
                ) {
                    Text("Delete", color = Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = BrownText)
                }
            }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            DetailBottomBar(
                selectedTab = DetailBottomTab.HOME,
                onHomeClick = onHomeClick,
                onProfileClick = onProfileClick,
                onCreateClick = onCreateClick,
                onDraftsClick = onDraftsClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            item {
                DetailTopBar(
                    onSettingsClick = onSettingsClick
                )
                HorizontalDivider(color = borderColor)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = entry.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .background(surfaceColor, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (entry.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = entry.photoUrl,
                                contentDescription = "Entry photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Text(
                                text = "Photo",
                                style = MaterialTheme.typography.headlineMedium,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = entry.previewText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )

                        if (isOwner) {
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier.clickable { onEditClick(entry.id) }
                            )

                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = textColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showDeleteDialog = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    DetailTagBubbleRow(tags = entry.tags)

                    Spacer(modifier = Modifier.height(18.dp))

                    if (locationSharingEnabled) {
                        MiniMapBox(
                            locationName = entry.location,
                            latLng = latLng,
                            cameraPositionState = cameraPositionState,
                            onOpenMapsApp = {
                                openExternalMapsApp(context, entry.location)
                            }
                        )
                    } else {
                        LockedMapBox(
                            message = if (currentUserId.isBlank()) {
                                "Log in to use map access."
                            } else {
                                "Turn on Location Sharing in Settings to use the map."
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(surfaceColor, RoundedCornerShape(20.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                .clickable(enabled = currentUserId.isNotBlank() && !isLiking) {
                                    if (currentUserId.isBlank()) {
                                        likeError = "You must be logged in to like posts."
                                    } else {
                                        isLiking = true
                                        likeError = ""

                                        FirestoreRepository.toggleLike(
                                            entryId = entry.id,
                                            userId = currentUserId,
                                            username = currentUsername,
                                            isCurrentlyLiked = isLiked,
                                            onSuccess = {
                                                isLiking = false
                                            },
                                            onError = { message ->
                                                isLiking = false
                                                likeError = message
                                            }
                                        )
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = if (isLiking) "Updating..." else if (isLiked) "Liked" else "Like",
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Text(
                            text = "$likeCount likes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    if (likeError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = likeError,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB3261E)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LockedMapBox(
    message: String
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(surfaceColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DetailTopBar(
    onSettingsClick: () -> Unit
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = "memo",
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
                    text = { Text("Settings", color = textColor) },
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
private fun MiniMapBox(
    locationName: String,
    latLng: LatLng,
    cameraPositionState: CameraPositionState,
    onOpenMapsApp: () -> Unit
) {
    val markerState = remember(latLng) {
        MarkerState(position = latLng)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(1.dp, FieldBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                zoomGesturesEnabled = true,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
                mapToolbarEnabled = false
            ),
            onMapClick = { onOpenMapsApp() }
        ) {
            Marker(
                state = markerState,
                title = locationName
            )
        }
    }
}

private enum class DetailBottomTab {
    HOME,
    PROFILE
}

@Composable
private fun DetailBottomBar(
    selectedTab: DetailBottomTab,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onDraftsClick: () -> Unit
) {
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
        DetailBottomBarIcon(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            selected = selectedTab == DetailBottomTab.HOME,
            onClick = onHomeClick
        )

        DetailBottomBarIcon(
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

        DetailBottomBarIcon(
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
            onClick = onCreateClick
        )

        DetailBottomBarIcon(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            selected = selectedTab == DetailBottomTab.PROFILE,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun DetailBottomBarIcon(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTagBubbleRow(tags: List<String>) {
    val tagColors = listOf(
        Color(0xFFFFE0B2),
        Color(0xFFC8E6C9),
        Color(0xFFBBDEFB),
        Color(0xFFF8BBD0),
        Color(0xFFDCCCEB)
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEachIndexed { index, tag ->
            DetailTagBubble(
                text = "#$tag",
                backgroundColor = tagColors[index % tagColors.size]
            )
        }
    }
}

@Composable
private fun DetailTagBubble(
    text: String,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = BrownText
        )
    }
}

private fun openExternalMapsApp(context: Context, locationName: String) {
    val encoded = URLEncoder.encode(locationName, StandardCharsets.UTF_8.toString())
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
    context.startActivity(intent)
}

private fun locationToLatLng(location: String): LatLng {
    return when (location) {
        "Chicago, Illinois" -> LatLng(41.8781, -87.6298)
        "Aspen, Colorado" -> LatLng(39.1911, -106.8175)
        "Miami, Florida" -> LatLng(25.7617, -80.1918)
        "Fairfax, VA" -> LatLng(38.8462, -77.3064)
        "Outerbanks, NC" -> LatLng(35.5582, -75.4665)
        "outerbanks, NC" -> LatLng(35.5582, -75.4665)
        "NYC, NY" -> LatLng(40.7128, -74.0060)
        else -> LatLng(39.8283, -98.5795)
    }
}