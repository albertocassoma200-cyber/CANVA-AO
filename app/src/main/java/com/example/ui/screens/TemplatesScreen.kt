package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.audio.LocalSoundEffects
import com.example.core.model.CategoryEntry
import com.example.core.model.DesignTemplate
import com.example.core.model.UiState
import com.example.core.model.UserProfile
import com.example.ui.components.DesignCanvasThumbnail
import com.example.ui.components.parseComposeColor
import com.example.ui.theme.OrangeAccent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    templatesState: UiState<List<DesignTemplate>>,
    categoriesState: UiState<List<CategoryEntry>>,
    favoriteIds: Set<String>,
    initialCategoryFilter: String = "Todos",
    userProfile: UserProfile? = null,
    onToggleFavorite: (DesignTemplate) -> Unit,
    onUseTemplate: (DesignTemplate) -> Unit
) {
    val sound = LocalSoundEffects.current
    val authUser = remember { Firebase.auth.currentUser }
    val userAvatarUrl = userProfile?.photoUrl?.takeIf { it.isNotBlank() } ?: authUser?.photoUrl?.toString()
    val userDisplayName = userProfile?.displayName?.takeIf { it.isNotBlank() } ?: authUser?.displayName
    val userHandle = remember(userProfile, authUser) {
        val em = userProfile?.email?.takeIf { it.isNotBlank() } ?: authUser?.email.orEmpty()
        val prefix = if (em.contains("@")) {
            em.substringBefore("@")
        } else {
            (userDisplayName ?: "criador").lowercase().replace(Regex("[^a-z0-9]"), "")
        }
        "@${prefix.ifBlank { "criador" }}"
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember(initialCategoryFilter) { mutableStateOf(initialCategoryFilter) }
    var priceFilter by remember { mutableStateOf("Todos") } // Todos, Imagens, Vídeos, Gratuitos, Premium, Favoritos
    var ratioFilter by remember { mutableStateOf("Todas") } // Todas, 1:1, 3:4, 4:5, 16:9, 9:16
    var previewTemplate by remember { mutableStateOf<DesignTemplate?>(null) }

    val templates = (templatesState as? UiState.Success)?.data.orEmpty()
    val categories = listOf("Todos") + ((categoriesState as? UiState.Success)?.data.orEmpty().map { it.name })

    val filtered = remember(templates, searchQuery, selectedCategory, priceFilter, ratioFilter, favoriteIds) {
        templates.filter { tpl ->
            val matchQuery = searchQuery.isBlank() ||
                tpl.name.contains(searchQuery, ignoreCase = true) ||
                tpl.category.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == "Todos" || tpl.category.equals(selectedCategory, ignoreCase = true)
            val matchPrice = when (priceFilter) {
                "Imagens" -> tpl.templateType != "video"
                "Vídeos" -> tpl.templateType == "video"
                "Gratuitos" -> !tpl.premium
                "Premium" -> tpl.premium
                "Favoritos" -> tpl.id in favoriteIds
                else -> true
            }
            val matchRatio = ratioFilter == "Todas" || tpl.effectiveAspectRatio() == ratioFilter
            matchQuery && matchCat && matchPrice && matchRatio
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Templates",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar templates...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = OrangeAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("templates_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Todos", "Imagens", "Vídeos", "Gratuitos", "Premium", "Favoritos")) { filter ->
                    FilterChip(
                        selected = priceFilter == filter,
                        onClick = {
                            sound?.playTap()
                            priceFilter = filter
                        },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Todas", "1:1", "3:4", "4:5", "16:9", "9:16")) { ratio ->
                    FilterChip(
                        selected = ratioFilter == ratio,
                        onClick = {
                            sound?.playTap()
                            ratioFilter = ratio
                        },
                        label = { Text(if (ratio == "Todas") "Proporção: Todas" else ratio) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            sound?.playTap()
                            selectedCategory = cat
                        },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum template encontrado para os filtros atuais.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { tpl ->
                    val isFav = tpl.id in favoriteIds
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                sound?.playTap()
                                previewTemplate = tpl
                            }
                            .testTag("templates_grid_item_${tpl.id}"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.82f)
                            ) {
                                DesignCanvasThumbnail(
                                    width = tpl.width,
                                    height = tpl.height,
                                    backgroundColorHex = tpl.backgroundColorHex,
                                    backgroundGradientHex = tpl.backgroundGradientHex,
                                    layers = tpl.layers,
                                    userAvatarUrl = userAvatarUrl,
                                    userDisplayName = userDisplayName,
                                    userHandle = userHandle,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        color = if (tpl.premium) OrangeAccent else Color.Black.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (tpl.premium) "PRO" else "Grátis",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (tpl.templateType == "video") {
                                        Surface(
                                            color = Color(0xFF4F46E5).copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = "VÍDEO",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = tpl.effectiveAspectRatio(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        sound?.playTap()
                                        onToggleFavorite(tpl)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(36.dp)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Favorito",
                                        tint = if (isFav) OrangeAccent else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = tpl.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tpl.category} • ${tpl.effectiveAspectRatio()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Template Preview Sheet
    previewTemplate?.let { tpl ->
        val isFav = tpl.id in favoriteIds
        ModalBottomSheet(
            onDismissRequest = { previewTemplate = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tpl.name, style = MaterialTheme.typography.titleLarge)
                        val typeSummary = if (tpl.templateType == "video") "Vídeo (${tpl.videoClips.size} cenas)" else "Imagem"
                        Text(
                            "${tpl.category} • ${tpl.effectiveAspectRatio()} (${tpl.width} × ${tpl.height} px) • $typeSummary • ${if (tpl.premium) "Premium" else "Gratuito"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        sound?.playTap()
                        onToggleFavorite(tpl)
                    }) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFav) OrangeAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DesignCanvasThumbnail(
                        width = tpl.width,
                        height = tpl.height,
                        backgroundColorHex = tpl.backgroundColorHex,
                        backgroundGradientHex = tpl.backgroundGradientHex,
                        layers = tpl.layers,
                        userAvatarUrl = userAvatarUrl,
                        userDisplayName = userDisplayName,
                        userHandle = userHandle,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Cores:", style = MaterialTheme.typography.labelLarge)
                    tpl.colorsUsed.take(5).forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(parseComposeColor(hex, OrangeAccent))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fontes: ${tpl.fontsUsed.joinToString(", ").ifBlank { "Plus Jakarta Sans" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        sound?.playCreate()
                        previewTemplate = null
                        onUseTemplate(tpl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("use_template_button")
                ) {
                    Icon(if (tpl.templateType == "video") Icons.Filled.PlayArrow else Icons.Filled.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (tpl.templateType == "video") "Personalizar Vídeo no Editor" else "Personalizar Template no Editor",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
