package com.fruitbilling.app.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import com.fruitbilling.app.data.repository.ProductRepository
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
    val deleteConfirmationProduct: Product? = null
)

class MenuViewModel(
    private val productRepository: ProductRepository
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
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            return MenuViewModel(productRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
