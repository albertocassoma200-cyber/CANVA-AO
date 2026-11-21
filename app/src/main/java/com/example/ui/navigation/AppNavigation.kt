package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.audio.LocalSoundEffects
import com.example.core.audio.SoundEffectsManager
import com.example.core.model.UiState
import com.example.core.model.UserCredits
import com.example.core.preferences.UserPreferencesDataStore
import com.example.data.repository.authStateFlow
import com.example.data.repository.signOut
import com.example.ui.components.CreateOptionsBottomSheet
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreditsScreen
import com.example.ui.screens.DesignEditorScreen
import com.example.ui.screens.EmailVerificationGateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.screens.VideoEditorScreen
import com.example.ui.theme.CanvaAoTheme
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OrangeAccent
import com.example.ui.viewmodel.AiStudioViewModel
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data class TemplatesRoute(val categoryFilter: String = "Todos")
@Serializable data object ProjectsRoute
@Serializable data class ProfileRoute(val openNotifications: Boolean = false)
@Serializable data object AiStudioRoute
@Serializable data object CreditsRoute
@Serializable data object DesignEditorRoute
@Serializable data object VideoEditorRoute
@Serializable data object AdminRoute

@Composable
fun CanvaAoRootApp() {
    val context = LocalContext.current
    val prefs = remember(context) { UserPreferencesDataStore(context.applicationContext) }
    val persistedSoundEnabled by prefs.soundEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val soundManager = remember(context) { SoundEffectsManager(context) }

    LaunchedEffect(persistedSoundEnabled) {
        soundManager.enabled = persistedSoundEnabled
    }
    DisposableEffect(soundManager) {
        onDispose { soundManager.release() }
    }

    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    CompositionLocalProvider(LocalSoundEffects provides soundManager) {
        val user = currentUser
        val credentialManager = remember(context) { CredentialManager.create(context) }
        val scope = rememberCoroutineScope()
        if (user == null) {
            CanvaAoTheme(themeMode = "dark") {
                AuthScreen(onAuthSuccess = {})
            }
        } else {
            val isGoogleAccount = remember(user.uid, user.providerData) {
                user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            }
            var emailVerifiedGatePassed by remember(user.uid, user.isEmailVerified) {
                mutableStateOf(user.isEmailVerified || isGoogleAccount)
            }
            if (!emailVerifiedGatePassed) {
                CanvaAoTheme(themeMode = "dark") {
                    EmailVerificationGateScreen(
                        email = user.email.orEmpty(),
                        onVerified = { emailVerifiedGatePassed = true },
                        onSignOut = { signOut(credentialManager, scope) }
                    )
                }
            } else {
                key(user.uid) {
                    val mainViewModel: MainViewModel = viewModel(
                        key = user.uid,
                        factory = MainViewModel.provideFactory(
                            userId = user.uid,
                            email = user.email.orEmpty()
                        )
                    )
                    val aiViewModel: AiStudioViewModel = viewModel(key = "ai_${user.uid}")
                    val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()

                    CanvaAoTheme(themeMode = themeMode) {
                        AuthenticatedAppShell(
                            mainViewModel = mainViewModel,
                            aiViewModel = aiViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    mainViewModel: MainViewModel,
    aiViewModel: AiStudioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val userProfileState by mainViewModel.userProfileState.collectAsStateWithLifecycle()
    val creditsState by mainViewModel.creditsState.collectAsStateWithLifecycle()
    val transactionsState by mainViewModel.transactionsState.collectAsStateWithLifecycle()
    val rewardAdsState by mainViewModel.rewardAdsState.collectAsStateWithLifecycle()
    val exportsState by mainViewModel.exportsState.collectAsStateWithLifecycle()
    val projectsState by mainViewModel.projectsState.collectAsStateWithLifecycle()
    val templatesState by mainViewModel.templatesState.collectAsStateWithLifecycle()
    val categoriesState by mainViewModel.categoriesState.collectAsStateWithLifecycle()
    val favoritesState by mainViewModel.favoritesState.collectAsStateWithLifecycle()
    val notificationsState by mainViewModel.notificationsState.collectAsStateWithLifecycle()
    val activeDesignProject by mainViewModel.activeDesignProject.collectAsStateWithLifecycle()
    val activeVideoProject by mainViewModel.activeVideoProject.collectAsStateWithLifecycle()
    val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val soundEnabled by mainViewModel.soundEnabled.collectAsStateWithLifecycle()
    val snackbarMessage by mainViewModel.snackbarMessage.collectAsStateWithLifecycle()

    val soundManager = LocalSoundEffects.current ?: remember(context) { SoundEffectsManager(context) }
    LaunchedEffect(soundEnabled) {
        soundManager.enabled = soundEnabled
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearSnackbar()
        }
    }

    val profile = (userProfileState as? UiState.Success)?.data
    if (profile?.blocked == true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Filled.Block, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(56.dp))
                Text("Conta suspensa", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "O acesso a esta conta foi suspenso pela administração do CANVA AO.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { signOut(credentialManager, scope) }) {
                    Text("Terminar Sessão")
                }
            }
        }
        return
    }

    val creditsBalance = (creditsState as? UiState.Success)?.data?.balance ?: UserCredits().balance
    val favoriteIds = remember(favoritesState) {
        ((favoritesState as? UiState.Success)?.data.orEmpty()).map { it.itemId }.toSet()
    }
    var showCreateSheet by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val isTopLevelTab = currentDest?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<TemplatesRoute>() ||
            dest.hasRoute<ProjectsRoute>() ||
            dest.hasRoute<ProfileRoute>()
    } ?: true

    CompositionLocalProvider(LocalSoundEffects provides soundManager) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (isTopLevelTab && !isWideScreen) {
                    CanvaAoBottomBar(
                        isHome = currentDest?.hasRoute<HomeRoute>() == true,
                        isTemplates = currentDest?.hasRoute<TemplatesRoute>() == true,
                        isProjects = currentDest?.hasRoute<ProjectsRoute>() == true,
                        isProfile = currentDest?.hasRoute<ProfileRoute>() == true,
                        onSelectHome = {
                            soundManager.playTap()
                            navController.navigate(HomeRoute) {
                                launchSingleTop = true
                                popUpTo(HomeRoute) { inclusive = false }
                            }
                        },
                        onSelectTemplates = {
                            soundManager.playTap()
                            navController.navigate(TemplatesRoute("Todos")) { launchSingleTop = true }
                        },
                        onOpenCreate = {
                            soundManager.playCreate()
                            showCreateSheet = true
                        },
                        onSelectProjects = {
                            soundManager.playTap()
                            navController.navigate(ProjectsRoute) { launchSingleTop = true }
                        },
                        onSelectProfile = {
                            soundManager.playTap()
                            navController.navigate(ProfileRoute(false)) { launchSingleTop = true }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelTab && isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FloatingActionButton(
                            onClick = {
                                soundManager.playCreate()
                                showCreateSheet = true
                            },
                            containerColor = OrangeAccent,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Criar")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        NavigationRailItem(
                            selected = currentDest?.hasRoute<HomeRoute>() == true,
                            onClick = {
                                soundManager.playTap()
                                navController.navigate(HomeRoute) { launchSingleTop = true }
                            },
                            icon = { Icon(Icons.Filled.Home, contentDescription = "Início") },
                            label = { Text("Início") }
                        )
                        NavigationRailItem(
                            selected = currentDest?.hasRoute<TemplatesRoute>() == true,
                            onClick = {
                                soundManager.playTap()
                                navController.navigate(TemplatesRoute("Todos")) { launchSingleTop = true }
                            },
                            icon = { Icon(Icons.Filled.GridView, contentDescription = "Modelos") },
                            label = { Text("Modelos") }
                        )
                        NavigationRailItem(
                            selected = currentDest?.hasRoute<ProjectsRoute>() == true,
                            onClick = {
                                soundManager.playTap()
                                navController.navigate(ProjectsRoute) { launchSingleTop = true }
                            },
                            icon = { Icon(Icons.Filled.FolderOpen, contentDescription = "Projetos") },
                            label = { Text("Projetos") }
                        )
                        NavigationRailItem(
                            selected = currentDest?.hasRoute<ProfileRoute>() == true,
                            onClick = {
                                soundManager.playTap()
                                navController.navigate(ProfileRoute(false)) { launchSingleTop = true }
                            },
                            icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
                            label = { Text("Perfil") }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                userProfileState = userProfileState,
                                creditsState = creditsState,
                                categoriesState = categoriesState,
                                templatesState = templatesState,
                                projectsState = projectsState,
                                notificationsState = notificationsState,
                                favoriteIds = favoriteIds,
                                onCreditsClick = { navController.navigate(CreditsRoute) },
                                onNotificationsClick = { navController.navigate(ProfileRoute(openNotifications = true)) },
                                onOpenCreateSheet = { showCreateSheet = true },
                                onOpenAiStudio = { navController.navigate(AiStudioRoute) },
                                onCategorySelected = { cat -> navController.navigate(TemplatesRoute(cat)) },
                                onTemplateClick = { tpl ->
                                    mainViewModel.openTemplateInEditor(tpl)
                                    if (tpl.templateType == "video") {
                                        navController.navigate(VideoEditorRoute)
                                    } else {
                                        navController.navigate(DesignEditorRoute)
                                    }
                                },
                                onToggleTemplateFavorite = { tpl ->
                                    mainViewModel.toggleTemplateFavorite(tpl.id, tpl.id in favoriteIds)
                                },
                                onProjectClick = { proj ->
                                    mainViewModel.openExistingProject(proj)
                                    if (proj.projectType == "video") {
                                        navController.navigate(VideoEditorRoute)
                                    } else {
                                        navController.navigate(DesignEditorRoute)
                                    }
                                },
                                onSeeAllTemplates = { navController.navigate(TemplatesRoute("Todos")) },
                                onSeeAllProjects = { navController.navigate(ProjectsRoute) }
                            )
                        }

                        composable<TemplatesRoute> { entry ->
                            val args = entry.toRoute<TemplatesRoute>()
                            TemplatesScreen(
                                templatesState = templatesState,
                                categoriesState = categoriesState,
                                favoriteIds = favoriteIds,
                                initialCategoryFilter = args.categoryFilter,
                                userProfile = profile,
                                onToggleFavorite = { tpl ->
                                    mainViewModel.toggleTemplateFavorite(tpl.id, tpl.id in favoriteIds)
                                },
                                onUseTemplate = { tpl ->
                                    mainViewModel.openTemplateInEditor(tpl)
                                    if (tpl.templateType == "video") {
                                        navController.navigate(VideoEditorRoute)
                                    } else {
                                        navController.navigate(DesignEditorRoute)
                                    }
                                }
                            )
                        }

                        composable<ProjectsRoute> {
                            ProjectsScreen(
                                projectsState = projectsState,
                                onOpenProject = { proj ->
                                    mainViewModel.openExistingProject(proj)
                                    if (proj.projectType == "video") {
                                        navController.navigate(VideoEditorRoute)
                                    } else {
                                        navController.navigate(DesignEditorRoute)
                                    }
                                },
                                onCreateNew = { showCreateSheet = true },
                                onDuplicateProject = mainViewModel::duplicateProject,
                                onRenameProject = mainViewModel::renameProject,
                                onToggleFavorite = mainViewModel::toggleProjectFavorite,
                                onDeleteProject = mainViewModel::deleteProject
                            )
                        }

                        composable<ProfileRoute> { entry ->
                            val args = entry.toRoute<ProfileRoute>()
                            var openNotifs by remember(args.openNotifications) { mutableStateOf(args.openNotifications) }
                            ProfileScreen(
                                userProfileState = userProfileState,
                                notificationsState = notificationsState,
                                themeMode = themeMode,
                                language = language,
                                soundEnabled = soundEnabled,
                                isAdmin = mainViewModel.isUserAdmin(profile),
                                openNotificationsInitially = openNotifs,
                                onNotificationsOpened = { openNotifs = false },
                                onNavigateProjects = { navController.navigate(ProjectsRoute) },
                                onNavigateCredits = { navController.navigate(CreditsRoute) },
                                onNavigateAdmin = { navController.navigate(AdminRoute) },
                                onUpdateProfile = mainViewModel::updateProfileDetails,
                                onChangeTheme = mainViewModel::setThemeMode,
                                onChangeLanguage = mainViewModel::setLanguage,
                                onToggleSound = mainViewModel::setSoundEnabled,
                                onMarkNotificationRead = mainViewModel::markNotificationRead,
                                onSubmitReport = mainViewModel::submitReport,
                                onSignOut = { signOut(credentialManager, scope) },
                                onDeleteAccount = {
                                    mainViewModel.deleteAccount {
                                        signOut(credentialManager, scope)
                                    }
                                }
                            )
                        }

                        composable<AiStudioRoute> {
                            AiStudioScreen(
                                aiViewModel = aiViewModel,
                                currentUserId = mainViewModel.currentUserId,
                                onBack = {
                                    soundManager.playTap()
                                    navController.popBackStack()
                                },
                                onOpenAiProjectInEditor = { proj ->
                                    soundManager.playCreate()
                                    mainViewModel.openAiProjectInEditor(proj)
                                    navController.navigate(DesignEditorRoute)
                                },
                                onSaveAsTemplate = { tpl ->
                                    soundManager.playSuccess()
                                    mainViewModel.saveAiDesignAsTemplate(tpl)
                                },
                                onShowMessage = mainViewModel::showMessage
                            )
                        }

                        composable<CreditsRoute> {
                            CreditsScreen(
                                creditsState = creditsState,
                                transactionsState = transactionsState,
                                rewardAdsState = rewardAdsState,
                                exportsState = exportsState,
                                onBack = { navController.popBackStack() },
                                onClaimRewardedAd = mainViewModel::claimRewardedAd,
                                onShowMessage = mainViewModel::showMessage
                            )
                        }

                        composable<DesignEditorRoute> {
                            DesignEditorScreen(
                                initialProject = activeDesignProject,
                                creditsBalance = creditsBalance,
                                onBack = { navController.popBackStack() },
                                onSaveToCloud = { proj -> mainViewModel.saveProjectToCloud(proj) },
                                onReserveExport = mainViewModel::reserveExport,
                                onCompleteExport = mainViewModel::completeExport,
                                onFailExport = mainViewModel::failOrCancelExport,
                                onGoToCredits = { navController.navigate(CreditsRoute) },
                                onShowMessage = mainViewModel::showMessage
                            )
                        }

                        composable<VideoEditorRoute> {
                            VideoEditorScreen(
                                initialProject = activeVideoProject,
                                creditsBalance = creditsBalance,
                                onBack = { navController.popBackStack() },
                                onSaveToCloud = { proj -> mainViewModel.saveProjectToCloud(proj) },
                                onReserveExport = mainViewModel::reserveExport,
                                onCompleteExport = mainViewModel::completeExport,
                                onFailExport = mainViewModel::failOrCancelExport,
                                onGoToCredits = { navController.navigate(CreditsRoute) },
                                onShowMessage = mainViewModel::showMessage
                            )
                        }

                        composable<AdminRoute> {
                            AdminScreen(
                                viewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateOptionsBottomSheet(
            onDismiss = { showCreateSheet = false },
            onSelectDesignFormat = { format, prefix ->
                soundManager.playCreate()
                mainViewModel.openNewDesignFormat(format, prefix)
                navController.navigate(DesignEditorRoute)
            },
            onSelectCustomDimensions = { title, w, h ->
                soundManager.playCreate()
                mainViewModel.openCustomDesign(title, w, h)
                navController.navigate(DesignEditorRoute)
            },
            onSelectVideoEditor = {
                soundManager.playCreate()
                mainViewModel.openNewVideoProject()
                navController.navigate(VideoEditorRoute)
            }
        )
    }
    }
}

@Composable
private fun CanvaAoBottomBar(
    isHome: Boolean,
    isTemplates: Boolean,
    isProjects: Boolean,
    isProfile: Boolean,
    onSelectHome: () -> Unit,
    onSelectTemplates: () -> Unit,
    onOpenCreate: () -> Unit,
    onSelectProjects: () -> Unit,
    onSelectProfile: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTabItem(
                icon = Icons.Filled.Home,
                label = "Início",
                selected = isHome,
                tag = "nav_home",
                onClick = onSelectHome
            )
            BottomNavTabItem(
                icon = Icons.Filled.GridView,
                label = "Modelos",
                selected = isTemplates,
                tag = "nav_templates",
                onClick = onSelectTemplates
            )

            // Center Raised Orange "+" Button matching mockup
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .clickable(onClick = onOpenCreate)
                    .testTag("nav_create")
            ) {
                FloatingActionButton(
                    onClick = onOpenCreate,
                    containerColor = OrangeAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Criar")
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Criar",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = OrangeAccent
                )
            }

            BottomNavTabItem(
                icon = Icons.Filled.FolderOpen,
                label = "Projetos",
                selected = isProjects,
                tag = "nav_projects",
                onClick = onSelectProjects
            )
            BottomNavTabItem(
                icon = Icons.Filled.Person,
                label = "Perfil",
                selected = isProfile,
                tag = "nav_profile",
                onClick = onSelectProfile
            )
        }
    }
}

@Composable
private fun BottomNavTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val color = if (selected) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = color
        )
    }
}
