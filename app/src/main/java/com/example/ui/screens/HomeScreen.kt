package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.example.R
import com.example.core.audio.LocalSoundEffects
import com.example.core.model.CategoryEntry
import com.example.core.model.DesignProject
import com.example.core.model.DesignTemplate
import com.example.core.model.NotificationItem
import com.example.core.model.UiState
import com.example.core.model.UserCredits
import com.example.core.model.UserProfile
import com.example.ui.components.CanvaAoLogoMark
import com.example.ui.components.CanvaAoWordmark
import com.example.ui.components.CreditPillBadge
import com.example.ui.components.DesignCanvasThumbnail
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.OrangeBright
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun HomeScreen(
    userProfileState: UiState<UserProfile?>,
    creditsState: UiState<UserCredits>,
    categoriesState: UiState<List<CategoryEntry>>,
    templatesState: UiState<List<DesignTemplate>>,
    projectsState: UiState<List<DesignProject>>,
    notificationsState: UiState<List<NotificationItem>>,
    favoriteIds: Set<String>,
    onCreditsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOpenCreateSheet: () -> Unit,
    onOpenAiStudio: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onTemplateClick: (DesignTemplate) -> Unit,
    onToggleTemplateFavorite: (DesignTemplate) -> Unit,
    onProjectClick: (DesignProject) -> Unit,
    onSeeAllTemplates: () -> Unit,
    onSeeAllProjects: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val sound = LocalSoundEffects.current
    val profile = (userProfileState as? UiState.Success)?.data
    val authUser = remember { Firebase.auth.currentUser }
    val userAvatarUrl = profile?.photoUrl?.takeIf { it.isNotBlank() } ?: authUser?.photoUrl?.toString()
    val userDisplayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: authUser?.displayName
    val userHandle = remember(profile, authUser) {
        val em = profile?.email?.takeIf { it.isNotBlank() } ?: authUser?.email.orEmpty()
        val prefix = if (em.contains("@")) {
            em.substringBefore("@")
        } else {
            (userDisplayName ?: "criador").lowercase().replace(Regex("[^a-z0-9]"), "")
        }
        "@${prefix.ifBlank { "criador" }}"
    }
    val credits = (creditsState as? UiState.Success)?.data?.balance ?: 50L
    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
    val allTemplates = (templatesState as? UiState.Success)?.data.orEmpty()
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val unreadNotifs = ((notificationsState as? UiState.Success)?.data.orEmpty()).count { !it.read }

    val filteredTemplates = remember(allTemplates, searchQuery) {
        if (searchQuery.isBlank()) allTemplates
        else allTemplates.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Top Header with Greeting, Wordmark, Notifications & Credits Pill
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CanvaAoWordmark()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CreditPillBadge(balance = credits, onClick = {
                            sound?.playTap()
                            onCreditsClick()
                        })
                        IconButton(
                            onClick = {
                                sound?.playTap()
                                onNotificationsClick()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge(containerColor = OrangeAccent) {
                                            Text("$unreadNotifs", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notificações",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                Column {
                    val firstName = profile?.displayName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Criador"
                    Text(
                        text = "Olá, $firstName!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Vamos criar algo incrível hoje?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquisar templates, flyers, vídeos...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Pesquisar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
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
                        .testTag("home_search_input")
                )
            }
        }

        // 2. Hero Promotional Banner with matching studio artwork
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(NavyPrimary)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_ideas),
                    contentDescription = "Dê vida às suas ideias",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to NavyPrimary.copy(alpha = 0.94f),
                                0.56f to ElectricIndigo.copy(alpha = 0.80f),
                                1.0f to Color.Transparent
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.76f)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Dê vida às suas ideias",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Designs profissionais, vídeos MP4 e IA no CANVA AO",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            sound?.playCreate()
                            onOpenCreateSheet()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_hero_create_button")
                    ) {
                        Text("Começar", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 3. AI Studio Quick Access Card (with CANVA AO app icon)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        sound?.playTap()
                        onOpenAiStudio()
                    }
                    .border(1.dp, CyanGlow.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
                    .testTag("home_ai_studio_card"),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(NavyPrimary, ElectricIndigo)))
                            .border(1.dp, CyanGlow.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CanvaAoLogoMark(size = 32.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("AI Studio", style = MaterialTheme.typography.titleMedium)
                            Surface(
                                color = ElectricIndigo.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "IA + Templates",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanGlow,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Crie templates editáveis com fotos reais, gere imagens e legendas em segundos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Abrir AI Studio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Categories Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Categorias",
                    actionLabel = "Ver tudo",
                    onAction = onSeeAllTemplates
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        Surface(
                            modifier = Modifier
                                .width(92.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    sound?.playTap()
                                    onCategorySelected(cat.name)
                                }
                                .testTag("home_category_${cat.name}"),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OrangeAccent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIconFor(cat.name),
                                        contentDescription = cat.name,
                                        tint = OrangeAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Recommended Templates
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Templates em destaque",
                    actionLabel = "Ver tudo",
                    onAction = {
                        sound?.playTap()
                        onSeeAllTemplates()
                    }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTemplates.take(12), key = { it.id }) { tpl ->
                        TemplateHorizontalCard(
                            template = tpl,
                            isFavorite = tpl.id in favoriteIds,
                            userAvatarUrl = userAvatarUrl,
                            userDisplayName = userDisplayName,
                            userHandle = userHandle,
                            onClick = {
                                sound?.playTap()
                                onTemplateClick(tpl)
                            },
                            onToggleFavorite = {
                                sound?.playTap()
                                onToggleTemplateFavorite(tpl)
                            }
                        )
                    }
                }
            }
        }

        // 5b. Video Templates (1:1, 3:4, 4:5, 16:9, 9:16)
        item {
            val videoTemplates = remember(filteredTemplates) {
                filteredTemplates.filter { it.templateType == "video" }
            }
            if (videoTemplates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Templates de Vídeo • 1:1 a 9:16",
                        actionLabel = "Ver tudo",
                        onAction = {
                            sound?.playTap()
                            onSeeAllTemplates()
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(videoTemplates, key = { "vid_${it.id}" }) { tpl ->
                            TemplateHorizontalCard(
                                template = tpl,
                                isFavorite = tpl.id in favoriteIds,
                                userAvatarUrl = userAvatarUrl,
                                userDisplayName = userDisplayName,
                                userHandle = userHandle,
                                onClick = {
                                    sound?.playTap()
                                    onTemplateClick(tpl)
                                },
                                onToggleFavorite = {
                                    sound?.playTap()
                                    onToggleTemplateFavorite(tpl)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 6. Recent Projects
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Projetos recentes",
                    actionLabel = "Meus Projetos",
                    onAction = onSeeAllProjects
                )
                if (projects.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenCreateSheet),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ainda sem projetos guardados", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Toque aqui para criar o seu primeiro design ou editar um vídeo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(projects.take(6), key = { it.id }) { project ->
                            Surface(
                                modifier = Modifier
                                    .width(146.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onProjectClick(project) },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 2.dp
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                    ) {
                                        if (project.projectType == "video") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Brush.linearGradient(listOf(ElectricIndigo, Color(0xFF0A1628)))),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.Movie, contentDescription = null, tint = OrangeBright, modifier = Modifier.size(36.dp))
                                            }
                                        } else {
                                            DesignCanvasThumbnail(
                                                width = project.width,
                                                height = project.height,
                                                backgroundColorHex = project.backgroundColorHex,
                                                backgroundGradientHex = project.backgroundGradientHex,
                                                layers = project.layers,
                                                userAvatarUrl = userAvatarUrl,
                                                userDisplayName = userDisplayName,
                                                userHandle = userHandle,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = project.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (project.projectType == "video") "Vídeo MP4" else project.formatName,
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
        }

        // 7. Popular Designs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Designs populares",
                    actionLabel = "Explorar",
                    onAction = onSeeAllTemplates
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTemplates.reversed().take(6), key = { "pop_${it.id}" }) { tpl ->
                        TemplateHorizontalCard(
                            template = tpl,
                            isFavorite = tpl.id in favoriteIds,
                            userAvatarUrl = userAvatarUrl,
                            userDisplayName = userDisplayName,
                            userHandle = userHandle,
                            onClick = { onTemplateClick(tpl) },
                            onToggleFavorite = { onToggleTemplateFavorite(tpl) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onAction) {
            Text(actionLabel, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TemplateHorizontalCard(
    template: DesignTemplate,
    isFavorite: Boolean,
    userAvatarUrl: String?,
    userDisplayName: String?,
    userHandle: String?,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sound = LocalSoundEffects.current
    Surface(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("template_card_${template.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
            ) {
                DesignCanvasThumbnail(
                    width = template.width,
                    height = template.height,
                    backgroundColorHex = template.backgroundColorHex,
                    backgroundGradientHex = template.backgroundGradientHex,
                    layers = template.layers,
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
                    if (template.premium) {
                        Surface(
                            color = OrangeAccent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PRO",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (template.templateType == "video") {
                        Surface(
                            color = ElectricIndigo.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "VÍDEO",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                        text = template.effectiveAspectRatio(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                IconButton(
                    onClick = {
                        sound?.playTap()
                        onToggleFavorite()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(34.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) OrangeAccent else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${template.category} • ${template.effectiveAspectRatio()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

fun categoryIconFor(name: String): ImageVector = when (name.lowercase()) {
    "instagram", "facebook", "tiktok" -> Icons.Filled.PhotoLibrary
    "youtube" -> Icons.Filled.Movie
    "publicidade", "promoções" -> Icons.Filled.Campaign
    "negócios" -> Icons.Filled.BusinessCenter
    "notícias" -> Icons.Filled.Newspaper
    "futebol" -> Icons.Filled.SportsSoccer
    "música" -> Icons.Filled.MusicNote
    "eventos", "aniversários", "convites" -> Icons.Filled.Celebration
    "restaurantes" -> Icons.Filled.Restaurant
    "moda", "beleza" -> Icons.Filled.Checkroom
    "tecnologia" -> Icons.Filled.Workspaces
    "educação" -> Icons.Filled.School
    else -> Icons.Filled.Star
}
