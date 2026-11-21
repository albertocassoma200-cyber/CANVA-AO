package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.audio.LocalSoundEffects
import com.example.data.repository.EmailPasswordProviderDisabledException
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.onGoogleSignInClicked
import com.example.data.repository.registerWithEmailPassword
import com.example.data.repository.reloadAndCheckEmailVerified
import com.example.data.repository.sendPasswordReset
import com.example.data.repository.sendVerificationEmail
import com.example.data.repository.signInWithEmailPassword
import com.example.ui.components.CanvaAoLogoMark
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sound = LocalSoundEffects.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var authTab by remember { mutableIntStateOf(0) } // 0 = Entrar, 1 = Criar Conta
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProviderDisabledError by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingSession = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingSession = false
            },
            scope = scope
        )
    }

    fun launchGoogleAuth() {
        sound?.playTap()
        errorMessage = null
        isProviderDisabledError = false
        infoMessage = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                sound?.playSuccess()
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
            }
        )
    }

    fun submitEmailAuth() {
        sound?.playTap()
        errorMessage = null
        isProviderDisabledError = false
        infoMessage = null
        isLoading = true
        scope.launch {
            val res = if (authTab == 0) {
                signInWithEmailPassword(email, password)
            } else {
                registerWithEmailPassword(fullName, email, password)
            }
            isLoading = false
            res.fold(
                onSuccess = {
                    sound?.playSuccess()
                    onAuthSuccess()
                },
                onFailure = { err ->
                    isProviderDisabledError = err is EmailPasswordProviderDisabledException
                    errorMessage = err.localizedMessage ?: "Falha na autenticação."
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
            .imePadding()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val topWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width * 0.65f, 0f)
                quadraticTo(size.width * 0.45f, size.height * 0.14f, 0f, size.height * 0.16f)
                close()
            }
            drawPath(topWave, OrangeAccent)

            val bottomWave = Path().apply {
                moveTo(size.width, size.height)
                lineTo(size.width * 0.3f, size.height)
                quadraticTo(size.width * 0.55f, size.height * 0.85f, size.width, size.height * 0.78f)
                close()
            }
            drawPath(bottomWave, OrangeAccent)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CanvaAoLogoMark(size = 58.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CANVA ",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AO",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = OrangeAccent
                        )
                    }
                    Text(
                        text = "Crie. Edite. Inspire-se.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FeatureChip(Icons.Filled.Brush, "Design")
                        FeatureChip(Icons.Filled.Movie, "Vídeos")
                        FeatureChip(Icons.Filled.AutoAwesome, "AI Studio")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isCheckingSession) {
                        CircularProgressIndicator(
                            color = OrangeAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A verificar sessão...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Google Sign-In Button
                        Button(
                            onClick = { launchGoogleAuth() },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = NavyDeep
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("google_sign_in_button")
                        ) {
                            GoogleLogoBadge(size = 24.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Entrar com Google",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "ou com Gmail e senha",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        TabRow(
                            selectedTabIndex = authTab,
                            containerColor = Color.Transparent,
                            contentColor = OrangeAccent
                        ) {
                            Tab(
                                selected = authTab == 0,
                                onClick = {
                                    sound?.playTap()
                                    authTab = 0
                                    errorMessage = null
                                    isProviderDisabledError = false
                                },
                                text = { Text("Entrar") }
                            )
                            Tab(
                                selected = authTab == 1,
                                onClick = {
                                    sound?.playTap()
                                    authTab = 1
                                    errorMessage = null
                                    isProviderDisabledError = false
                                },
                                text = { Text("Criar Conta") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (authTab == 1) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Nome completo") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail / Gmail") },
                            placeholder = { Text("exemplo@gmail.com") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Palavra-passe") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    sound?.playTap()
                                    passwordVisible = !passwordVisible
                                }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar palavra-passe" else "Mostrar palavra-passe"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )

                        if (authTab == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        sound?.playTap()
                                        showRecoveryDialog = true
                                    },
                                    modifier = Modifier.testTag("forgot_password_button")
                                ) {
                                    Text("Esqueci a palavra-passe", color = OrangeAccent)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ao criar conta, enviamos o link de confirmação para o seu Gmail.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = { submitEmailAuth() },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("email_auth_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = if (authTab == 0) "Entrar com Gmail" else "Criar Conta",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    if (!infoMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = infoMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                if (isProviderDisabledError) {
                                    Text(
                                        text = "Projeto Firebase: gen-lang-client-0304900576",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = { launchGoogleAuth() },
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        GoogleLogoBadge(size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Entrar agora com Google")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sessão protegida com Firebase Auth & App Check",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showRecoveryDialog) {
        var resetEmail by remember(email) { mutableStateOf(email) }
        var resetBusy by remember { mutableStateOf(false) }
        var resetError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!resetBusy) showRecoveryDialog = false },
            icon = { Icon(Icons.Filled.LockReset, contentDescription = null, tint = OrangeAccent) },
            title = { Text("Recuperar Palavra-passe") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Introduza o seu Gmail para enviarmos o e-mail de recuperação diretamente para a sua caixa de entrada.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Seu Gmail / E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!resetError.isNullOrBlank()) {
                        Text(
                            text = resetError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !resetBusy,
                    onClick = {
                        sound?.playTap()
                        resetBusy = true
                        resetError = null
                        scope.launch {
                            sendPasswordReset(resetEmail).fold(
                                onSuccess = {
                                    sound?.playSuccess()
                                    resetBusy = false
                                    showRecoveryDialog = false
                                    infoMessage = "E-mail de recuperação enviado para ${resetEmail.trim()}! Verifique o seu Gmail."
                                },
                                onFailure = { err ->
                                    resetBusy = false
                                    resetError = err.localizedMessage ?: "Não foi possível enviar o e-mail."
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White)
                ) {
                    Text(if (resetBusy) "A enviar..." else "Enviar E-mail")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !resetBusy,
                    onClick = { showRecoveryDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EmailVerificationGateScreen(
    email: String,
    onVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sound = LocalSoundEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    reloadAndCheckEmailVerified().onSuccess { verified ->
                        if (verified) {
                            sound?.playSuccess()
                            onVerified()
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(OrangeAccent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MarkEmailRead,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = "Confirme o seu Gmail",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Enviámos a verificação para $email. Abra o seu Gmail, confirme a conta e depois toque abaixo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else SuccessGreen,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    enabled = !busy,
                    onClick = {
                        sound?.playTap()
                        busy = true
                        statusMessage = null
                        scope.launch {
                            reloadAndCheckEmailVerified().fold(
                                onSuccess = { verified ->
                                    busy = false
                                    if (verified) {
                                        sound?.playSuccess()
                                        onVerified()
                                    } else {
                                        isError = true
                                        statusMessage = "Ainda não foi confirmado. Verifique a caixa de entrada ou spam no Gmail."
                                    }
                                },
                                onFailure = { err ->
                                    busy = false
                                    isError = true
                                    statusMessage = err.localizedMessage ?: "Erro ao verificar estado."
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verify_email_check_button")
                ) {
                    Text(if (busy) "A verificar..." else "Já confirmei no Gmail")
                }

                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        sound?.playTap()
                        busy = true
                        scope.launch {
                            sendVerificationEmail().fold(
                                onSuccess = {
                                    sound?.playSuccess()
                                    busy = false
                                    isError = false
                                    statusMessage = "Novo e-mail de verificação enviado para $email!"
                                },
                                onFailure = { err ->
                                    busy = false
                                    isError = true
                                    statusMessage = err.localizedMessage ?: "Erro ao reenviar."
                                }
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reenviar e-mail para o Gmail")
                }

                TextButton(
                    onClick = {
                        sound?.playTap()
                        try {
                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_APP_EMAIL)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            isError = false
                            statusMessage = "Abra manualmente a app Gmail no seu dispositivo."
                        }
                    }
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir app de E-mail / Gmail")
                }

                TextButton(
                    onClick = {
                        sound?.playTap()
                        onSignOut()
                    }
                ) {
                    Text("Usar outra conta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun GoogleLogoBadge(size: Dp = 26.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val stroke = this.size.minDimension * 0.22f
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = Color(0xFFEA4335),
                startAngle = -140f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 140f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 45f,
                sweepAngle = 95f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -10f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(this.size.width * 0.50f, this.size.height * 0.50f),
                end = Offset(this.size.width * 0.94f, this.size.height * 0.50f),
                strokeWidth = stroke,
                cap = StrokeCap.Butt
            )
        }
    }
}

@Composable
private fun FeatureChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = OrangeAccent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = OrangeAccent, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
