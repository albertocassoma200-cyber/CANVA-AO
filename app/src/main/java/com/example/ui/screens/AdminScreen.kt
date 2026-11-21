package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.DesignLayer
import com.example.core.model.DesignTemplate
import com.example.core.model.LayerType
import com.example.core.model.UiState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.MainViewModel
import java.util.UUID

@Composable
fun AdminScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val usersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val exportsState by viewModel.adminExportsState.collectAsStateWithLifecycle()
    val adsState by viewModel.adminRewardAdsState.collectAsStateWithLifecycle()
    val reportsState by viewModel.adminReportsState.collectAsStateWithLifecycle()
    val templatesState by viewModel.templatesState.collectAsStateWithLifecycle()

    val users = (usersState as? UiState.Success)?.data.orEmpty()
    val exports = (exportsState as? UiState.Success)?.data.orEmpty()
    val ads = (adsState as? UiState.Success)?.data.orEmpty()
    val reports = (reportsState as? UiState.Success)?.data.orEmpty()
    val templates = (templatesState as? UiState.Success)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                    Text("Painel Administrativo", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = OrangeAccent
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Estatísticas") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Utilizadores") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Templates") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Catálogo & Avisos") })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Denúncias") })
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { StatCard("Utilizadores Registados", "${users.size}") }
                        item { StatCard("Templates Ativos", "${templates.size}") }
                        item { StatCard("Exportações Totais", "${exports.size}") }
                        item { StatCard("Anúncios Recompensados", "${ads.size}") }
                        item { StatCard("Denúncias Abertas", "${reports.count { it.status == "open" }}") }
                    }
                }

                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(users, key = { it.id.ifBlank { it.userId } }) { user ->
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.adminGrantCredits(user.userId, 10L) },
                                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                                        ) { Text("+10 Créditos") }
                                        OutlinedButton(
                                            onClick = { viewModel.adminToggleBlockUser(user.userId, !user.blocked) }
                                        ) {
                                            Icon(Icons.Filled.Block, null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (user.blocked) "Desbloquear" else "Bloquear")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    var tplName by remember { mutableStateOf("") }
                    var tplCategory by remember { mutableStateOf("Publicidade") }
                    var tplHeadline by remember { mutableStateOf("NOVO MODELO") }
                    var tplPremium by remember { mutableStateOf(false) }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Publicar Novo Template", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(value = tplName, onValueChange = { tplName = it }, label = { Text("Nome do Template") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = tplCategory, onValueChange = { tplCategory = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = tplHeadline, onValueChange = { tplHeadline = it }, label = { Text("Título Editável") }, modifier = Modifier.fillMaxWidth())
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Template Premium (PRO)")
                                        Switch(checked = tplPremium, onCheckedChange = { tplPremium = it })
                                    }
                                    Button(
                                        onClick = {
                                            if (tplName.isNotBlank()) {
                                                viewModel.adminSaveTemplate(
                                                    DesignTemplate(
                                                        name = tplName,
                                                        category = tplCategory,
                                                        width = 1080L,
                                                        height = 1080L,
                                                        backgroundColorHex = "#0A1628",
                                                        backgroundGradientHex = "#FF7A00",
                                                        layers = listOf(
                                                            DesignLayer(
                                                                layerId = "adm_${UUID.randomUUID().toString().take(6)}",
                                                                type = LayerType.TEXT,
                                                                name = "Título",
                                                                xFraction = 0.5f,
                                                                yFraction = 0.5f,
                                                                widthFraction = 0.85f,
                                                                heightFraction = 0.25f,
                                                                text = tplHeadline,
                                                                fontSizeSp = 38f,
                                                                bold = true
                                                            ).toMap()
                                                        ),
                                                        fontsUsed = listOf("plus_jakarta_sans"),
                                                        colorsUsed = listOf("#0A1628", "#FF7A00", "#FFFFFF"),
                                                        premium = tplPremium,
                                                        published = true
                                                    )
                                                )
                                                tplName = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Add, null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Publicar Template na Nuvem")
                                    }
                                }
                            }
                        }

                        items(templates.filterNot { it.id.startsWith("seed_") }, key = { it.id }) { tpl ->
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tpl.name, style = MaterialTheme.typography.titleSmall)
                                        Text("${tpl.category} • ${if (tpl.premium) "PRO" else "Grátis"}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { viewModel.adminDeleteTemplate(tpl.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remover template", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    var catName by remember { mutableStateOf("") }
                    var assetName by remember { mutableStateOf("") }
                    var fontName by remember { mutableStateOf("") }
                    var notifTitle by remember { mutableStateOf("") }
                    var notifBody by remember { mutableStateOf("") }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Enviar Notificação Global", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = notifBody, onValueChange = { notifBody = it }, label = { Text("Mensagem") }, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        onClick = {
                                            if (notifTitle.isNotBlank() && notifBody.isNotBlank()) {
                                                viewModel.adminBroadcastNotification(notifTitle, notifBody)
                                                notifTitle = ""
                                                notifBody = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.NotificationsActive, null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enviar a Todos")
                                    }
                                }
                            }
                        }
                        item {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Nova Categoria", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Nome da categoria") }, modifier = Modifier.fillMaxWidth())
                                    Button(onClick = {
                                        if (catName.isNotBlank()) {
                                            viewModel.adminCreateCategory(catName, "sparkles")
                                            catName = ""
                                        }
                                    }) { Text("Adicionar Categoria") }
                                }
                            }
                        }
                        item {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Novo Recurso / Fonte", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(value = assetName, onValueChange = { assetName = it }, label = { Text("Nome do sticker/recurso") }, modifier = Modifier.fillMaxWidth())
                                    Button(onClick = {
                                        if (assetName.isNotBlank()) {
                                            viewModel.adminCreateAsset(assetName, "sticker", "Geral", "#FF7A00", "star")
                                            assetName = ""
                                        }
                                    }) { Text("Adicionar Recurso") }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(value = fontName, onValueChange = { fontName = it }, label = { Text("Nome da fonte") }, modifier = Modifier.fillMaxWidth())
                                    Button(onClick = {
                                        if (fontName.isNotBlank()) {
                                            viewModel.adminCreateFont(fontName, "plus_jakarta_sans", "Display")
                                            fontName = ""
                                        }
                                    }) { Text("Registar Fonte") }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reports, key = { it.id }) { rep ->
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rep.reason, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "Estado: ${rep.status}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (rep.status == "resolved") SuccessGreen else OrangeAccent
                                        )
                                    }
                                    if (rep.status != "resolved") {
                                        FilterChip(
                                            selected = false,
                                            onClick = { viewModel.adminResolveReport(rep.id) },
                                            label = { Text("Resolver") },
                                            leadingIcon = { Icon(Icons.Filled.CheckCircle, null) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = OrangeAccent
            )
        }
    }
}
