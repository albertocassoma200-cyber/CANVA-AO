package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.example.core.model.CreditTransaction
import com.example.core.model.ExportRecord
import com.example.core.model.RewardAdLog
import com.example.core.model.UiState
import com.example.core.model.UserCredits
import com.example.engine.ads.RewardedAdController
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.OrangeBright
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreditsScreen(
    creditsState: UiState<UserCredits>,
    transactionsState: UiState<List<CreditTransaction>>,
    rewardAdsState: UiState<List<RewardAdLog>>,
    exportsState: UiState<List<ExportRecord>>,
    onBack: () -> Unit,
    onClaimRewardedAd: (adUnitId: String, idempotencyKey: String, onDone: (Boolean, String) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val adController = remember { RewardedAdController() }
    val credits = (creditsState as? UiState.Success)?.data ?: UserCredits()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val rewardAds = (rewardAdsState as? UiState.Success)?.data.orEmpty()
    val exports = (exportsState as? UiState.Success)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeAdKey by remember { mutableStateOf<String?>(null) }
    var claimingAd by remember { mutableStateOf(false) }

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
                    Text(
                        text = "Minha Carteira",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
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
            // Balance Hero Card (matching mockup)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(22.dp),
                color = NavyPrimary,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Seu saldo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Text(
                                text = "${credits.balance} Créditos",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(OrangeAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = "Saldo de Créditos",
                                tint = OrangeBright,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Ganhos: +${credits.totalEarned}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen
                        )
                        Text(
                            text = "Usados: -${credits.totalSpent}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            activeAdKey = adController.generateIdempotencyKey()
                        },
                        enabled = !claimingAd,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("watch_rewarded_ad_button")
                    ) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assistir Anúncio (+2 Créditos)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tabela: Exportar Imagem = 2 créditos • Exportar Vídeo MP4 = 4 créditos",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = OrangeAccent
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Histórico") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Anúncios") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Exportações") })
            }

            when (selectedTab) {
                0 -> {
                    if (transactions.isEmpty()) {
                        EmptyTabMessage("Sem movimentos de créditos registados.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(transactions, key = { it.id }) { tx ->
                                val positive = tx.amount >= 0L
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background((if (positive) SuccessGreen else OrangeAccent).copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (positive) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (positive) SuccessGreen else OrangeAccent
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.description, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                formatTimestamp(tx.createdAt?.toDate()),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = if (positive) "+${tx.amount} créd." else "${tx.amount} créd.",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (positive) SuccessGreen else ErrorRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (rewardAds.isEmpty()) {
                        EmptyTabMessage("Ainda não assistiu a nenhum anúncio recompensado.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(rewardAds, key = { it.id }) { ad ->
                                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Anúncio Recompensado", style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                formatTimestamp(ad.createdAt?.toDate()),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text("+${ad.rewardAmount} créd.", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (exports.isEmpty()) {
                        EmptyTabMessage("Nenhuma exportação registada ainda.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(exports, key = { it.id }) { exp ->
                                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = null, tint = OrangeAccent)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exp.projectTitle, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${exp.exportType.uppercase()} • ${exp.resolution} • ${exp.status}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = if (exp.status == "completed") "-${exp.creditsCost} créd." else "0 créd.",
                                            color = if (exp.status == "completed") OrangeAccent else SuccessGreen,
                                            fontWeight = FontWeight.Bold
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

    // Rewarded Ad Playback Modal (must finish full 5s to earn +2 credits)
    activeAdKey?.let { key ->
        RewardedAdPlayerDialog(
            adUnitId = RewardedAdController.TEST_REWARDED_AD_UNIT_ID,
            onDismissEarly = {
                activeAdKey = null
                onShowMessage("Anúncio fechado antes do fim. Nenhum crédito adicionado.")
            },
            onCompleted = {
                activeAdKey = null
                claimingAd = true
                onClaimRewardedAd(RewardedAdController.TEST_REWARDED_AD_UNIT_ID, key) { _, msg ->
                    claimingAd = false
                    onShowMessage(msg)
                }
            }
        )
    }
}

@Composable
private fun RewardedAdPlayerDialog(
    adUnitId: String,
    onDismissEarly: () -> Unit,
    onCompleted: () -> Unit
) {
    var remainingSec by remember { mutableIntStateOf(RewardedAdController.REWARD_DURATION_SECONDS) }
    val totalSec = RewardedAdController.REWARD_DURATION_SECONDS

    LaunchedEffect(Unit) {
        while (remainingSec > 0) {
            delay(1000L)
            remainingSec -= 1
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (remainingSec == 0) onCompleted() else onDismissEarly()
        },
        title = { Text("Anúncio Patrocinado") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(NavyPrimary, NavySurfaceElevated))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = OrangeBright, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("CANVA AO Creative Network", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Unidade: ${adUnitId.takeLast(12)}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                LinearProgressIndicator(
                    progress = { (totalSec - remainingSec).toFloat() / totalSec.toFloat() },
                    color = OrangeAccent,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (remainingSec > 0) {
                        "Recompensa de +2 créditos em ${remainingSec}s..."
                    } else {
                        "Anúncio concluído! Toque abaixo para receber +2 créditos."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCompleted,
                enabled = remainingSec == 0,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text("Receber +2 Créditos")
            }
        },
        dismissButton = {
            if (remainingSec > 0) {
                TextButton(onClick = onDismissEarly) { Text("Fechar (Sem créditos)") }
            }
        }
    )
}

@Composable
private fun EmptyTabMessage(msg: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTimestamp(date: Date?): String {
    if (date == null) return "Agora mesmo"
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
}
