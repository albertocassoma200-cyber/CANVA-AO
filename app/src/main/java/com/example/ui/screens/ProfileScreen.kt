package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.core.audio.LocalSoundEffects
import com.example.core.model.NotificationItem
import com.example.core.model.UiState
import com.example.core.model.UserProfile
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.OrangeAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfileState: UiState<UserProfile?>,
    notificationsState: UiState<List<NotificationItem>>,
    themeMode: String,
    language: String,
    soundEnabled: Boolean,
    isAdmin: Boolean,
    openNotificationsInitially: Boolean = false,
    onNotificationsOpened: () -> Unit = {},
    onNavigateProjects: () -> Unit,
    onNavigateCredits: () -> Unit,
    onNavigateAdmin: () -> Unit,
    onUpdateProfile: (displayName: String, photoUrl: String?, notificationsEnabled: Boolean) -> Unit,
    onChangeTheme: (String) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onSubmitReport: (targetId: String, reason: String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val sound = LocalSoundEffects.current
    val profile = (userProfileState as? UiState.Success)?.data
    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val unreadCount = notifications.count { !it.read }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember(openNotificationsInitially) { mutableStateOf(openNotificationsInitially) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (openNotificationsInitially) {
        onNotificationsOpened()
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            onUpdateProfile(
                profile?.displayName ?: "Criador CANVA AO",
                uri.toString(),
                profile?.notificationsEnabled ?: true
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Perfil", style = MaterialTheme.typography.headlineMedium)
                IconButton(
                    onClick = {
                        sound?.playTap()
                        showSettingsSheet = true
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                }
            }
        }

        // User Avatar & Identity
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(104.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(NavySurfaceElevated)
                            .clickable {
                                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile?.photoUrl.isNullOrBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(profile?.photoUrl),
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Avatar",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(OrangeAccent)
                            .clickable { showEditNameDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Editar perfil",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = profile?.displayName?.ifBlank { "Criador CANVA AO" } ?: "Criador CANVA AO",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = profile?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Main Menu Card matching mockup
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 2.dp
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Filled.FolderOpen,
                        label = "Meus Projetos",
                        onClick = onNavigateProjects,
                        tag = "profile_projects_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileMenuRow(
                        icon = Icons.Filled.AccountBalanceWallet,
                        label = "Carteira de Créditos",
                        onClick = onNavigateCredits,
                        tag = "profile_credits_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileMenuRow(
                        icon = Icons.Filled.Notifications,
                        label = "Notificações",
                        badgeCount = unreadCount,
                        onClick = { showNotificationsSheet = true },
                        tag = "profile_notifications_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileMenuRow(
                        icon = Icons.Filled.Settings,
                        label = "Configurações",
                        onClick = { showSettingsSheet = true },
                        tag = "profile_settings_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        label = "Ajuda e Suporte",
                        onClick = { showSupportDialog = true },
                        tag = "profile_support_row"
                    )
                    if (isAdmin) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileMenuRow(
                            icon = Icons.Filled.AdminPanelSettings,
                            label = "Painel Administrativo",
                            tint = OrangeAccent,
                            onClick = onNavigateAdmin,
                            tag = "profile_admin_row"
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        label = "Sair",
                        tint = OrangeAccent,
                        onClick = onSignOut,
                        tag = "profile_sign_out_row"
                    )
                }
            }
        }

        item {
            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = ErrorRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Eliminar Conta", color = ErrorRed)
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        var nameDraft by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Editar Nome do Perfil") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Nome completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(nameDraft, null, profile?.notificationsEnabled ?: true)
                    showEditNameDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Cancelar") } }
        )
    }

    // Settings Sheet matching mockup ("Configurações")
    if (showSettingsSheet) {
        var notifDraft by remember(profile) { mutableStateOf(profile?.notificationsEnabled ?: true) }
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Configurações", style = MaterialTheme.typography.titleLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Language, contentDescription = null)
                        Text("Idioma")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = language == "pt", onClick = {
                            sound?.playTap()
                            onChangeLanguage("pt")
                        }, label = { Text("Português") })
                        FilterChip(selected = language == "en", onClick = {
                            sound?.playTap()
                            onChangeLanguage("en")
                        }, label = { Text("English") })
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.DarkMode, contentDescription = null)
                        Text("Tema")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = themeMode == "dark", onClick = {
                            sound?.playTap()
                            onChangeTheme("dark")
                        }, label = { Text("Modo Escuro") })
                        FilterChip(selected = themeMode == "light", onClick = {
                            sound?.playTap()
                            onChangeTheme("light")
                        }, label = { Text("Modo Claro") })
                        FilterChip(selected = themeMode == "system", onClick = {
                            sound?.playTap()
                            onChangeTheme("system")
                        }, label = { Text("Sistema") })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Notifications, contentDescription = null)
                        Text("Notificações")
                    }
                    Switch(checked = notifDraft, onCheckedChange = {
                        sound?.playTap()
                        notifDraft = it
                    })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null)
                        Text("Som")
                    }
                    Switch(checked = soundEnabled, onCheckedChange = { enabled ->
                        sound?.enabled = enabled
                        if (enabled) sound?.playSuccess()
                        onToggleSound(enabled)
                    })
                }

                Text(
                    "CANVA AO v1.0 • Política de Privacidade & Termos de Uso aplicados ao armazenamento seguro em nuvem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        sound?.playSuccess()
                        onUpdateProfile(profile?.displayName ?: "Criador CANVA AO", null, notifDraft)
                        showSettingsSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Alterações", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Notifications Sheet
    if (showNotificationsSheet) {
        ModalBottomSheet(onDismissRequest = { showNotificationsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Notificações", style = MaterialTheme.typography.titleLarge)
                if (notifications.isEmpty()) {
                    Text("Sem notificações no momento.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    notifications.take(15).forEach { notif ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMarkNotificationRead(notif.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Support & Report Dialog
    if (showSupportDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Ajuda e Suporte") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Descreva a sua dúvida ou problema") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (reason.isNotBlank()) {
                        onSubmitReport("app_support", reason)
                        showSupportDialog = false
                    }
                }) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { showSupportDialog = false }) { Text("Fechar") } }
        )
    }

    // Delete Account Confirm
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Conta?") },
            text = { Text("Todos os seus projetos e saldo de créditos serão removidos permanentemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    badgeCount: Int = 0,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = label, tint = tint)
            Text(label, style = MaterialTheme.typography.titleSmall, color = tint)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (badgeCount > 0) {
                Badge(containerColor = OrangeAccent) { Text("$badgeCount", color = Color.White) }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
