package com.fruitbilling.app.ui.menu

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fruitbilling.app.data.backup.CloudBackupManager
import com.fruitbilling.app.data.backup.GoogleAuthManager
import com.fruitbilling.app.data.backup.GoogleUserData
import com.fruitbilling.app.data.db.AppDatabase
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import com.fruitbilling.app.data.repository.ProductRepository
import com.fruitbilling.app.util.AppReleaseInfo
import com.fruitbilling.app.util.OtaUpdateManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class MenuUiState(
    val products: List<Product> = emptyList(),
    val editingProduct: Product? = null,
    val isAddDialogOpen: Boolean = false,
    val deleteConfirmationProduct: Product? = null,
    // Google Account & Cloud Backup state
    val googleUser: GoogleUserData? = null,
    val lastBackupTimestamp: Long = 0L,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    // OTA App Updates state
    val isCheckingUpdate: Boolean = false,
    val updateReleaseInfo: AppReleaseInfo? = null,
    val showUpdateDialog: Boolean = false
)

class MenuViewModel(
    private val productRepository: ProductRepository,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            productRepository.allProducts.collect { productList ->
                _uiState.value = _uiState.value.copy(products = productList)
            }
        }
    }

    fun initContextData(context: Context) {
        val user = GoogleAuthManager.getLastSignedInAccount(context)
        val lastTime = CloudBackupManager.getLastBackupTime(context)
        _uiState.value = _uiState.value.copy(
            googleUser = user,
            lastBackupTimestamp = lastTime
        )
    }

    fun onGoogleSignInSuccess(user: GoogleUserData) {
        _uiState.value = _uiState.value.copy(googleUser = user)
        viewModelScope.launch {
            _snackbarMessages.emit("Signed in as ${user.displayName ?: user.email}")
        }
    }

    fun onSignOut(context: Context) {
        GoogleAuthManager.signOut(context) {
            _uiState.value = _uiState.value.copy(googleUser = null)
            viewModelScope.launch {
                _snackbarMessages.emit("Signed out of Google account")
            }
        }
    }

    fun onBackupToDrive(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true)
            val result = CloudBackupManager.createBackupJson(context, database)
            result.onSuccess { backupFile ->
                val updatedTime = CloudBackupManager.getLastBackupTime(context)
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    lastBackupTimestamp = updatedTime
                )
                CloudBackupManager.saveToGoogleDriveOrShare(context, backupFile)
                _snackbarMessages.emit("Backup created! Choose Google Drive or save location.")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isBackingUp = false)
                _snackbarMessages.emit("Backup failed: ${error.message}")
            }
        }
    }

    fun onRestoreBackup(jsonString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true)
            val result = CloudBackupManager.restoreFromJson(jsonString, database)
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(isRestoring = false)
                _snackbarMessages.emit("Restored $count items successfully!")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isRestoring = false)
                _snackbarMessages.emit("Restore failed: ${error.message}")
            }
        }
    }

    fun onCheckForUpdates(userInitiated: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingUpdate = true)
            val result = OtaUpdateManager.checkForUpdates()
            result.onSuccess { releaseInfo ->
                _uiState.value = _uiState.value.copy(
                    isCheckingUpdate = false,
                    updateReleaseInfo = releaseInfo,
                    showUpdateDialog = releaseInfo.isUpdateAvailable
                )
                if (!releaseInfo.isUpdateAvailable && userInitiated) {
                    _snackbarMessages.emit("App is up to date (${releaseInfo.currentVersion})")
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isCheckingUpdate = false)
                if (userInitiated) {
                    _snackbarMessages.emit("Could not check for updates: ${error.message ?: "Network error"}")
                }
            }
        }
    }

    fun onDismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(showUpdateDialog = false)
    }

    fun onInstallUpdate(context: Context) {
        val info = _uiState.value.updateReleaseInfo ?: return
        val url = info.downloadUrl
        if (url != null) {
            _uiState.value = _uiState.value.copy(showUpdateDialog = false)
            OtaUpdateManager.startDownloadAndInstall(context, url, info.latestVersion)
            viewModelScope.launch {
                _snackbarMessages.emit("Downloading update ${info.latestVersion}... check notification bar.")
            }
        } else {
            viewModelScope.launch {
                _snackbarMessages.emit("Download link unavailable for ${info.latestVersion}")
            }
        }
    }

    fun onOpenAddDialog() {
        _uiState.value = _uiState.value.copy(isAddDialogOpen = true, editingProduct = null)
    }

    fun onOpenEditDialog(product: Product) {
        _uiState.value = _uiState.value.copy(editingProduct = product, isAddDialogOpen = false)
    }

    fun onCloseDialog() {
        _uiState.value = _uiState.value.copy(isAddDialogOpen = false, editingProduct = null)
    }

    fun onSaveProduct(
        id: Long?,
        name: String,
        priceText: String,
        unit: ProductUnit
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            viewModelScope.launch { _snackbarMessages.emit("Product name cannot be empty.") }
            return
        }

        val price = priceText.trim().toBigDecimalOrNull()
        if (price == null || price <= BigDecimal.ZERO) {
            viewModelScope.launch { _snackbarMessages.emit("Price must be greater than 0.") }
            return
        }

        viewModelScope.launch {
            if (id == null || id == 0L) {
                // Add
                productRepository.insertProduct(trimmedName, price, unit)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isAddDialogOpen = false)
                        _snackbarMessages.emit("Product '$trimmedName' added.")
                    }
                    .onFailure { error ->
                        _snackbarMessages.emit(error.message ?: "Could not add product.")
                    }
            } else {
                // Edit
                productRepository.updateProduct(id, trimmedName, price, unit)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(editingProduct = null)
                        _snackbarMessages.emit("Product '$trimmedName' updated.")
                    }
                    .onFailure { error ->
                        _snackbarMessages.emit(error.message ?: "Could not update product.")
                    }
            }
        }
    }

    fun onToggleActive(product: Product) {
        viewModelScope.launch {
            productRepository.setActive(product.id, active = !product.active)
                .onSuccess {
                    val label = if (product.active) "deactivated" else "activated"
                    _snackbarMessages.emit("Product '${product.name}' $label.")
                }
                .onFailure { error ->
                    _snackbarMessages.emit(error.message ?: "Could not update product.")
                }
        }
    }

    fun onPromptDelete(product: Product) {
        _uiState.value = _uiState.value.copy(deleteConfirmationProduct = product)
    }

    fun onCancelDelete() {
        _uiState.value = _uiState.value.copy(deleteConfirmationProduct = null)
    }

    fun onConfirmDelete() {
        val product = _uiState.value.deleteConfirmationProduct ?: return
        viewModelScope.launch {
            productRepository.deleteProduct(product.id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(deleteConfirmationProduct = null)
                    _snackbarMessages.emit("Product '${product.name}' deleted.")
                }
                .onFailure { error ->
                    _snackbarMessages.emit(error.message ?: "Could not delete product.")
                }
        }
    }
}

class MenuViewModelFactory(
    private val productRepository: ProductRepository,
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            return MenuViewModel(productRepository, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
