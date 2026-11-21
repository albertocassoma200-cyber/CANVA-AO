package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.model.CategoryEntry
import com.example.core.model.CreditTransaction
import com.example.core.model.DesignFormat
import com.example.core.model.DesignFormats
import com.example.core.model.DesignLayer
import com.example.core.model.DesignProject
import com.example.core.model.DesignTemplate
import com.example.core.model.ExportRecord
import com.example.core.model.FavoriteEntry
import com.example.core.model.FontEntry
import com.example.core.model.GraphicAsset
import com.example.core.model.LayerType
import com.example.core.model.NotificationItem
import com.example.core.model.ReportItem
import com.example.core.model.RewardAdLog
import com.example.core.model.TemplateSeedCatalog
import com.example.core.model.UiState
import com.example.core.model.UserCredits
import com.example.core.model.UserProfile
import com.example.core.preferences.UserPreferencesDataStore
import com.example.data.repository.CanvaAoRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val repository: CanvaAoRepository,
    private val prefs: UserPreferencesDataStore,
    val currentUserId: String,
    val currentUserEmail: String
) : ViewModel() {


    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserInitialized(user)
            }
        }
    }

    private fun <T> Flow<T>.toUiState(initial: UiState<T> = UiState.Loading): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream error", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Erro ao carregar dados."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = initial
            )

    val themeMode: StateFlow<String> = prefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), "dark")

    val language: StateFlow<String> = prefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), "pt")

    val soundEnabled: StateFlow<Boolean> = prefs.soundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState()

    val creditsState: StateFlow<UiState<UserCredits>> =
        repository.observeUserCredits(currentUserId).toUiState()

    val transactionsState: StateFlow<UiState<List<CreditTransaction>>> =
        repository.observeCreditTransactions(currentUserId).toUiState()

    val rewardAdsState: StateFlow<UiState<List<RewardAdLog>>> =
        repository.observeRewardAds(currentUserId).toUiState()

    val exportsState: StateFlow<UiState<List<ExportRecord>>> =
        repository.observeExports(currentUserId).toUiState()

    val projectsState: StateFlow<UiState<List<DesignProject>>> =
        repository.observeUserProjects(currentUserId).toUiState()

    val templatesState: StateFlow<UiState<List<DesignTemplate>>> =
        repository.observeTemplates()
            .map { remote ->
                val remoteIds = remote.map { it.id }.toSet()
                remote + TemplateSeedCatalog.builtInTemplates.filterNot { it.id in remoteIds }
            }
            .toUiState(UiState.Success(TemplateSeedCatalog.builtInTemplates))

    val categoriesState: StateFlow<UiState<List<CategoryEntry>>> =
        repository.observeCategories()
            .map { remote ->
                if (remote.isNotEmpty()) remote else TemplateSeedCatalog.allCategories.mapIndexed { idx, name ->
                    CategoryEntry(id = "cat_$idx", name = name, iconKey = name.lowercase(), sortOrder = idx.toLong())
                }
            }
            .toUiState()

    val favoritesState: StateFlow<UiState<List<FavoriteEntry>>> =
        repository.observeFavorites(currentUserId).toUiState()

    val notificationsState: StateFlow<UiState<List<NotificationItem>>> =
        repository.observeNotifications(currentUserId).toUiState()

    val assetsState: StateFlow<UiState<List<GraphicAsset>>> =
        repository.observeAssets().toUiState()

    val fontsState: StateFlow<UiState<List<FontEntry>>> =
        repository.observeFonts().toUiState()

    // Admin streams (collected only inside AdminScreen)
    val adminUsersState: StateFlow<UiState<List<UserProfile>>> by lazy {
        repository.observeAllUsersAdmin().toUiState()
    }
    val adminExportsState: StateFlow<UiState<List<ExportRecord>>> by lazy {
        repository.observeAllExportsAdmin().toUiState()
    }
    val adminRewardAdsState: StateFlow<UiState<List<RewardAdLog>>> by lazy {
        repository.observeAllRewardAdsAdmin().toUiState()
    }
    val adminReportsState: StateFlow<UiState<List<ReportItem>>> by lazy {
        repository.observeAllReportsAdmin().toUiState()
    }

    private val _activeDesignProject = MutableStateFlow(createBlankDesignProject(DesignFormats.presets.first()))
    val activeDesignProject: StateFlow<DesignProject> = _activeDesignProject.asStateFlow()

    private val _activeVideoProject = MutableStateFlow(createBlankVideoProject())
    val activeVideoProject: StateFlow<DesignProject> = _activeVideoProject.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun isUserAdmin(profile: UserProfile?): Boolean {
        return currentUserEmail.equals("albertocassoma200@gmail.com", ignoreCase = true) ||
            profile?.role == "admin"
    }

    fun openTemplateInEditor(template: DesignTemplate) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val firebaseUser = Firebase.auth.currentUser
        val resolvedLayers = TemplateSeedCatalog.resolveLayersWithUserProfile(
            layers = template.layers,
            avatarUrl = profile?.photoUrl?.takeIf { it.isNotBlank() } ?: firebaseUser?.photoUrl?.toString(),
            displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: firebaseUser?.displayName,
            email = profile?.email?.takeIf { it.isNotBlank() } ?: firebaseUser?.email ?: currentUserEmail
        )
        _activeDesignProject.value = DesignProject(
            id = "",
            userId = currentUserId,
            title = template.name,
            category = template.category,
            formatName = "${template.width}×${template.height}",
            width = template.width,
            height = template.height,
            backgroundColorHex = template.backgroundColorHex,
            backgroundGradientHex = template.backgroundGradientHex,
            layers = resolvedLayers,
            videoClips = emptyList(),
            projectType = "design",
            favorite = false,
            thumbnailText = template.name
        )
    }

    fun openNewDesignFormat(format: DesignFormat, titlePrefix: String = "Novo Design") {
        _activeDesignProject.value = createBlankDesignProject(format, titlePrefix)
    }

    fun openCustomDesign(title: String, width: Int, height: Int) {
        val format = DesignFormat("custom", "Personalizado", width.coerceIn(200, 4096), height.coerceIn(200, 4096), "Personalizado")
        _activeDesignProject.value = createBlankDesignProject(format, title.ifBlank { "Design Personalizado" })
    }

    fun openExistingProject(project: DesignProject) {
        if (project.projectType == "video") {
            _activeVideoProject.value = project
        } else {
            _activeDesignProject.value = project
        }
    }

    fun openAiProjectInEditor(project: DesignProject) {
        _activeDesignProject.value = project.copy(id = "", userId = currentUserId)
    }

    fun openNewVideoProject(title: String = "Novo Vídeo") {
        _activeVideoProject.value = createBlankVideoProject(title)
    }

    private fun createBlankDesignProject(format: DesignFormat, title: String = "Novo ${format.name}"): DesignProject {
        return DesignProject(
            id = "",
            userId = currentUserId,
            title = title,
            category = format.category,
            formatName = format.name,
            width = format.width.toLong(),
            height = format.height.toLong(),
            backgroundColorHex = "#0A1628",
            backgroundGradientHex = "#1A325C",
            layers = listOf(
                DesignLayer(
                    layerId = "txt_${UUID.randomUUID().toString().take(6)}",
                    type = LayerType.TEXT,
                    name = "Título",
                    xFraction = 0.5f,
                    yFraction = 0.45f,
                    widthFraction = 0.8f,
                    heightFraction = 0.2f,
                    text = "TOQUE PARA EDITAR",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 32f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap()
            ),
            videoClips = emptyList(),
            projectType = "design"
        )
    }

    private fun createBlankVideoProject(title: String = "Novo Vídeo"): DesignProject {
        return DesignProject(
            id = "",
            userId = currentUserId,
            title = title,
            category = "Vídeos",
            formatName = "Vertical 9:16",
            width = 1080L,
            height = 1920L,
            backgroundColorHex = "#07101F",
            layers = emptyList(),
            videoClips = emptyList(),
            projectType = "video"
        )
    }

    fun saveProjectToCloud(project: DesignProject, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.saveProject(project).fold(
                onSuccess = { savedId ->
                    val updated = project.copy(id = savedId)
                    if (project.projectType == "video") {
                        _activeVideoProject.value = updated
                    } else {
                        _activeDesignProject.value = updated
                    }
                    _snackbarMessage.value = "Projeto guardado na nuvem!"
                    onSaved(savedId)
                },
                onFailure = { e ->
                    _snackbarMessage.value = "Erro ao guardar: ${e.localizedMessage}"
                }
            )
        }
    }

    fun duplicateProject(project: DesignProject) {
        viewModelScope.launch {
            repository.duplicateProject(project).fold(
                onSuccess = { _snackbarMessage.value = "Projeto duplicado." },
                onFailure = { _snackbarMessage.value = "Erro ao duplicar: ${it.localizedMessage}" }
            )
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newTitle).fold(
                onSuccess = { _snackbarMessage.value = "Nome atualizado." },
                onFailure = { _snackbarMessage.value = "Erro ao renomear: ${it.localizedMessage}" }
            )
        }
    }

    fun toggleProjectFavorite(project: DesignProject) {
        viewModelScope.launch {
            repository.toggleProjectFavorite(project.id, !project.favorite)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId).fold(
                onSuccess = { _snackbarMessage.value = "Projeto eliminado." },
                onFailure = { _snackbarMessage.value = "Erro ao eliminar: ${it.localizedMessage}" }
            )
        }
    }

    fun toggleTemplateFavorite(templateId: String, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleTemplateFavorite(templateId, currentlyFavorite)
        }
    }

    fun claimRewardedAd(adUnitId: String, idempotencyKey: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.claimRewardedAdCredits(adUnitId, idempotencyKey).fold(
                onSuccess = { newBalance ->
                    _snackbarMessage.value = "+2 créditos adicionados! Saldo: $newBalance"
                    onDone(true, "Recebeu +2 créditos! Novo saldo: $newBalance")
                },
                onFailure = { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Falha ao validar recompensa."
                    onDone(false, e.localizedMessage ?: "Erro ao creditar recompensa.")
                }
            )
        }
    }

    suspend fun reserveExport(
        projectId: String,
        projectTitle: String,
        exportType: String,
        resolution: String,
        creditsCost: Long,
        fileName: String
    ): Result<String> = repository.reserveExport(projectId, projectTitle, exportType, resolution, creditsCost, fileName)

    suspend fun completeExport(
        exportId: String,
        creditsCost: Long,
        exportType: String,
        fileSizeBytes: Long
    ): Result<Long> = repository.completeExportWithCreditDeduction(exportId, creditsCost, exportType, fileSizeBytes)

    suspend fun failOrCancelExport(exportId: String, cancelled: Boolean) {
        repository.markExportFailedOrCancelled(exportId, cancelled)
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
            val profile = (userProfileState.value as? UiState.Success)?.data
            if (profile != null) {
                repository.updateProfile(
                    displayName = profile.displayName,
                    photoUrl = profile.photoUrl,
                    themeMode = mode,
                    language = profile.language,
                    notificationsEnabled = profile.notificationsEnabled
                )
            }
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            prefs.setLanguage(lang)
            val profile = (userProfileState.value as? UiState.Success)?.data
            if (profile != null) {
                repository.updateProfile(
                    displayName = profile.displayName,
                    photoUrl = profile.photoUrl,
                    themeMode = profile.themeMode,
                    language = lang,
                    notificationsEnabled = profile.notificationsEnabled
                )
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setSoundEnabled(enabled) }
    }

    fun updateProfileDetails(displayName: String, photoUrl: String?, notificationsEnabled: Boolean) {
        viewModelScope.launch {
            val profile = (userProfileState.value as? UiState.Success)?.data
            repository.updateProfile(
                displayName = displayName,
                photoUrl = photoUrl ?: profile?.photoUrl,
                themeMode = themeMode.value,
                language = language.value,
                notificationsEnabled = notificationsEnabled
            ).fold(
                onSuccess = { _snackbarMessage.value = "Perfil atualizado com sucesso!" },
                onFailure = { _snackbarMessage.value = "Erro ao atualizar perfil: ${it.localizedMessage}" }
            )
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun submitReport(targetId: String, reason: String) {
        viewModelScope.launch {
            repository.submitReport(targetId, reason).fold(
                onSuccess = { _snackbarMessage.value = "Relatório enviado à equipa." },
                onFailure = { _snackbarMessage.value = "Falha ao enviar relatório." }
            )
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccountDataAndAuth().fold(
                onSuccess = { onComplete() },
                onFailure = { _snackbarMessage.value = "Para eliminar a conta, volte a iniciar sessão recentemente." }
            )
        }
    }

    // --- Admin Actions ---

    fun adminToggleBlockUser(userId: String, blocked: Boolean) {
        viewModelScope.launch {
            repository.toggleUserBlockAdmin(userId, blocked).fold(
                onSuccess = { _snackbarMessage.value = if (blocked) "Utilizador bloqueado." else "Utilizador desbloqueado." },
                onFailure = { _snackbarMessage.value = "Erro admin: ${it.localizedMessage}" }
            )
        }
    }

    fun adminGrantCredits(userId: String, amount: Long) {
        viewModelScope.launch {
            repository.grantCreditsAdmin(userId, amount).fold(
                onSuccess = { _snackbarMessage.value = "+$amount créditos atribuídos." },
                onFailure = { _snackbarMessage.value = "Erro admin: ${it.localizedMessage}" }
            )
        }
    }

    fun adminSaveTemplate(template: DesignTemplate) {
        viewModelScope.launch {
            repository.saveTemplateAdmin(template).fold(
                onSuccess = { _snackbarMessage.value = "Template guardado!" },
                onFailure = { _snackbarMessage.value = "Erro ao guardar template: ${it.localizedMessage}" }
            )
        }
    }

    fun adminDeleteTemplate(templateId: String) {
        viewModelScope.launch {
            repository.deleteTemplateAdmin(templateId).fold(
                onSuccess = { _snackbarMessage.value = "Template removido." },
                onFailure = { _snackbarMessage.value = "Erro ao remover template: ${it.localizedMessage}" }
            )
        }
    }

    fun adminCreateCategory(name: String, iconKey: String) {
        viewModelScope.launch {
            repository.createCategoryAdmin(name, iconKey, 20L).fold(
                onSuccess = { _snackbarMessage.value = "Categoria '$name' criada!" },
                onFailure = { _snackbarMessage.value = "Erro: ${it.localizedMessage}" }
            )
        }
    }

    fun adminCreateAsset(name: String, assetType: String, category: String, colorHex: String, symbolKey: String) {
        viewModelScope.launch {
            repository.createAssetAdmin(name, assetType, category, colorHex, symbolKey).fold(
                onSuccess = { _snackbarMessage.value = "Recurso gráfico adicionado!" },
                onFailure = { _snackbarMessage.value = "Erro: ${it.localizedMessage}" }
            )
        }
    }

    fun adminCreateFont(name: String, familyKey: String, category: String) {
        viewModelScope.launch {
            repository.createFontAdmin(name, familyKey, category).fold(
                onSuccess = { _snackbarMessage.value = "Fonte registada!" },
                onFailure = { _snackbarMessage.value = "Erro: ${it.localizedMessage}" }
            )
        }
    }

    fun adminBroadcastNotification(title: String, message: String) {
        viewModelScope.launch {
            repository.broadcastNotificationAdmin(title, message).fold(
                onSuccess = { _snackbarMessage.value = "Notificação enviada a todos os utilizadores!" },
                onFailure = { _snackbarMessage.value = "Erro: ${it.localizedMessage}" }
            )
        }
    }

    fun adminResolveReport(reportId: String) {
        viewModelScope.launch {
            repository.resolveReportAdmin(reportId).fold(
                onSuccess = { _snackbarMessage.value = "Denúncia marcada como resolvida." },
                onFailure = { _snackbarMessage.value = "Erro: ${it.localizedMessage}" }
            )
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun provideFactory(userId: String, email: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                MainViewModel(
                    repository = CanvaAoRepository(app),
                    prefs = UserPreferencesDataStore(app),
                    currentUserId = userId,
                    currentUserEmail = email
                )
            }
        }
    }
}
