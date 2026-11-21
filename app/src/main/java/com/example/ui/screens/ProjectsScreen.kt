package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.audio.LocalSoundEffects
import com.example.core.model.DesignProject
import com.example.core.model.UiState
import com.example.ui.components.DesignCanvasThumbnail
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OrangeAccent

@Composable
fun ProjectsScreen(
    projectsState: UiState<List<DesignProject>>,
    onOpenProject: (DesignProject) -> Unit,
    onCreateNew: () -> Unit,
    onDuplicateProject: (DesignProject) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onToggleFavorite: (DesignProject) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    val sound = LocalSoundEffects.current
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Todos") }
    var renamingProject by remember { mutableStateOf<DesignProject?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deletingProject by remember { mutableStateOf<DesignProject?>(null) }

    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val filtered = remember(projects, searchQuery, filter) {
        projects.filter { p ->
            val matchQuery = searchQuery.isBlank() || p.title.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (filter) {
                "Designs" -> p.projectType == "design"
                "Vídeos" -> p.projectType == "video"
                "Favoritos" -> p.favorite
                else -> true
            }
            matchQuery && matchFilter
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Meus Projetos", style = MaterialTheme.typography.headlineMedium)
                Button(
                    onClick = {
                        sound?.playCreate()
                        onCreateNew()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("projects_new_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Novo")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar nos meus projetos...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = OrangeAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Todos", "Designs", "Vídeos", "Favoritos")) { chip ->
                    FilterChip(
                        selected = filter == chip,
                        onClick = {
                            sound?.playTap()
                            filter = chip
                        },
                        label = { Text(chip) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAccent,
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(56.dp))
                    Text("Nenhum projeto encontrado", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Crie um design gráfico ou vídeo e guarde na nuvem para aceder em qualquer dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { project ->
                    var menuOpen by remember { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenProject(project) }
                            .testTag("project_card_${project.id}"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                if (project.projectType == "video") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(ElectricIndigo, NavyPrimary))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Movie, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(32.dp))
                                    }
                                } else {
                                    DesignCanvasThumbnail(
                                        width = project.width,
                                        height = project.height,
                                        backgroundColorHex = project.backgroundColorHex,
                                        backgroundGradientHex = project.backgroundGradientHex,
                                        layers = project.layers,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (project.projectType == "video") {
                                        "Vídeo • ${project.videoClips.size} clipe(s)"
                                    } else {
                                        "${project.formatName} • ${project.width}×${project.height}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                sound?.playTap()
                                onToggleFavorite(project)
                            }) {
                                Icon(
                                    imageVector = if (project.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorito",
                                    tint = if (project.favorite) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box {
                                IconButton(onClick = {
                                    sound?.playTap()
                                    menuOpen = true
                                }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Opções")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Renomear") },
                                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) },
                                        onClick = {
                                            menuOpen = false
                                            renamingProject = project
                                            renameValue = project.title
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplicar") },
                                        leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                        onClick = {
                                            menuOpen = false
                                            onDuplicateProject(project)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuOpen = false
                                            deletingProject = project
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renamingProject?.let { proj ->
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            title = { Text("Renomear Projeto") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Novo nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameProject(proj.id, renameValue)
                    renamingProject = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) { Text("Cancelar") }
            }
        )
    }

    deletingProject?.let { proj ->
        AlertDialog(
            onDismissRequest = { deletingProject = null },
            title = { Text("Eliminar Projeto?") },
            text = { Text("Tem a certeza de que pretende eliminar \"${proj.title}\"? Esta ação é irreversível.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteProject(proj.id)
                        deletingProject = null
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deletingProject = null }) { Text("Cancelar") }
            }
        )
    }
}
