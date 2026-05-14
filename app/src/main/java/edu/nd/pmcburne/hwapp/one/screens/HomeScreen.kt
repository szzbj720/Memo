// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/screens/HomeScreen.kt
package edu.nd.pmcburne.hwapp.one.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import edu.nd.pmcburne.hwapp.one.data.FirestoreRepository

data class HomeEntry(
    val id: String,
    val title: String,
    val location: String,
    val date: String,
    val previewText: String,
    val tags: List<String>,
    val photoUrl: String
)

@Composable
fun HomeScreen(
    onEntryClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    val publicEntries = remember { mutableStateListOf<HomeEntry>() }

    DisposableEffect(Unit) {
        val listener = FirestoreRepository.listenToPublicEntries(
            onSuccess = { entries ->
                publicEntries.clear()
                publicEntries.addAll(entries)
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

    val filteredEntries = publicEntries.filter { entry ->
        val query = searchText.trim().lowercase()
        query.isEmpty() ||
                entry.title.lowercase().contains(query) ||
                entry.location.lowercase().contains(query) ||
                entry.previewText.lowercase().contains(query) ||
                entry.tags.any { tag -> tag.lowercase().contains(query) }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val textColor = MaterialTheme.colorScheme.onBackground

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            BottomHomeBar(
                selectedTab = BottomTab.HOME,
                onHomeClick = {},
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
                title = "memo",
                onSettingsClick = onSettingsClick
            )
            HorizontalDivider(color = borderColor)

            SearchBar(
                searchText = searchText,
                onSearchTextChange = { searchText = it }
            )

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
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
                                    text = "Loading public entries...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor
                                )
                            }
                        }
                    }

                    filteredEntries.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No public entries yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor
                                )
                            }
                        }
                    }

                    else -> {
                        items(filteredEntries, key = { it.id }) { entry ->
                            FeedEntryCard(
                                entry = entry,
                                onClick = { onEntryClick(entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTopBar(
    title: String,
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
            text = title,
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
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        placeholder = {
            Text(
                text = "Search public memories",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun FeedEntryCard(
    entry: HomeEntry,
    onClick: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(surfaceColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = entry.date,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "📍 ${entry.location}",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                TagBubbleRow(tags = entry.tags)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(92.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(backgroundColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (entry.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = entry.photoUrl,
                        contentDescription = "Entry photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Text(
                        text = "pic",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagBubbleRow(tags: List<String>) {
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
            TagBubble(
                text = "#$tag",
                backgroundColor = tagColors[index % tagColors.size]
            )
        }
    }
}

@Composable
private fun TagBubble(
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
            color = Color(0xFF3D2F26)
        )
    }
}

private enum class BottomTab {
    HOME,
    PROFILE
}

@Composable
private fun BottomHomeBar(
    selectedTab: BottomTab,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCreateClick: () -> Unit,
    onDraftsClick: () -> Unit
) {
    var showCreateMenu by rememberSaveable { mutableStateOf(false) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val selectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor)
            .background(surfaceColor)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarIcon(
            selected = selectedTab == BottomTab.HOME,
            selectedColor = selectedColor,
            onClick = onHomeClick
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
        }

        BottomBarIcon(
            selected = false,
            selectedColor = selectedColor,
            onClick = onDraftsClick
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = "Upload",
                tint = textColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Box {
            BottomBarIcon(
                selected = false,
                selectedColor = selectedColor,
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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Drafts",
                            tint = textColor
                        )
                    },
                    onClick = {
                        showCreateMenu = false
                        onDraftsClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Create new entry", color = textColor) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Entry",
                            tint = textColor
                        )
                    },
                    onClick = {
                        showCreateMenu = false
                        onCreateClick()
                    }
                )
            }
        }

        BottomBarIcon(
            selected = selectedTab == BottomTab.PROFILE,
            selectedColor = selectedColor,
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
private fun BottomBarIcon(
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
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