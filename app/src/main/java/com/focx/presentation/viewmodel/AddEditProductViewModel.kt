package com.focx.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.core.constants.AppConstants
import com.focx.domain.entity.Product
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.GetProductByIdUseCase
import com.focx.domain.usecase.SaveProductUseCase
import com.focx.domain.usecase.UpdateProductUseCase
import com.focx.presentation.intent.AddEditProductIntent
import com.focx.presentation.state.AddEditProductState
import com.focx.presentation.ui.screens.ProductFormData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class AddEditProductEffect {
    object NavigateBack : AddEditProductEffect()
    data class ShowMessage(val message: String) : AddEditProductEffect()
}

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val saveProductUseCase: SaveProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val getCurrentWalletAddressUseCase: GetCurrentWalletAddressUseCase
) : ViewModel() {

    private val productId: String? = savedStateHandle.get<String>("productId")
    
    private val _state = MutableStateFlow(AddEditProductState())
    val state: StateFlow<AddEditProductState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AddEditProductEffect>()
    val effect = _effect.asSharedFlow()

    init {
        if (productId != null) {
            loadProduct(productId)
        }
    }

    fun handleIntent(intent: AddEditProductIntent) {
        when (intent) {
            is AddEditProductIntent.UpdateFormData -> updateFormData(intent.formData)
            is AddEditProductIntent.SaveProduct -> saveProduct(intent.activityResultSender)
            is AddEditProductIntent.NavigateBack -> navigateBack()
        }
    }

    private fun loadProduct(productId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                getProductByIdUseCase(productId).collect { result ->
                    result.fold(
                        onSuccess = { product ->
                            if (product != null) {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    formData = ProductFormData(
                                        name = product.name,
                                        description = product.description,
                                        price = (product.price.toDouble() / 1000000).toString(), // Convert from micro units
                                        category = product.category,
                                        stock = product.stock.toString(),
                                        images = product.imageUrls,
                                        shippingOptions = product.shippingMethods
                                    ),
                                    originalProduct = product
                                )
                            } else {
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Product not found"
                                )
                                _effect.emit(AddEditProductEffect.ShowMessage("Product not found"))
                            }
                        },
                        onFailure = { exception ->
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Unknown error"
                            )
                            _effect.emit(AddEditProductEffect.ShowMessage("Failed to load product"))
                        }
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
                _effect.emit(AddEditProductEffect.ShowMessage("Failed to load product"))
            }
        }
    }

    private fun updateFormData(formData: ProductFormData) {
        _state.value = _state.value.copy(formData = formData)
    }

    private fun saveProduct(activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        val formData = _state.value.formData
        
        // Validate form
        if (!isFormValid(formData)) {
            viewModelScope.launch {
                _effect.emit(AddEditProductEffect.ShowMessage("Please fill all required fields"))
            }
            return
        }

        // Check wallet connection
        val walletAddress = getCurrentWalletAddressUseCase.execute()
        if (walletAddress == null) {
            viewModelScope.launch {
                _effect.emit(AddEditProductEffect.ShowMessage("Please connect your wallet first"))
            }
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val product = Product(
                    id = (productId?.toULongOrNull() ?: generateProductId()),
                    name = formData.name,
                    description = formData.description,
                    price = (formData.price.toDoubleOrNull()?.times(AppConstants.App.TOKEN_DECIMAL)?.toULong() ?: 0UL), // Convert to micro units
                    imageUrls = formData.images,
                    sellerId = walletAddress,
                    sellerName = "Default Seller", // TODO: Get from user profile
                    category = formData.category,
                    stock = formData.stock.toIntOrNull() ?: 0,
                    shippingFrom = formData.shippingOrigin,
                    shippingTo = formData.salesRegions,
                    shippingMethods = formData.shippingOptions,
                    rating = _state.value.originalProduct?.rating ?: 0.0F,
                    reviewCount = _state.value.originalProduct?.reviewCount ?: 0,
                    keywords = formData.keywords
                )

                // Use a separate context with SupervisorJob to survive app lifecycle changes
                withContext(Dispatchers.IO + SupervisorJob()) {
                    if (productId != null) {
                        updateProductUseCase(product, walletAddress, activityResultSender)
                    } else {
                        saveProductUseCase(product, walletAddress, activityResultSender)
                    }
                }
                
                // Only update UI if the coroutine is still active
                if (isActive) {
                    _state.value = _state.value.copy(isSaving = false)
                    if (productId != null) {
                        _effect.emit(AddEditProductEffect.ShowMessage("Product updated successfully"))
                    } else {
                        _effect.emit(AddEditProductEffect.ShowMessage("Product saved successfully"))
                    }
                    _effect.emit(AddEditProductEffect.NavigateBack)
                }
            } catch (e: CancellationException) {
                // Don't treat cancellation as an error - user might return to app
                com.focx.utils.Log.d("AddEditProductViewModel", "Product save cancelled due to lifecycle change: ${e.message}")
                // Reset saving state even on cancellation
                _state.value = _state.value.copy(isSaving = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false)
                _effect.emit(AddEditProductEffect.ShowMessage("Failed to save product: ${e.message}"))
            }
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.emit(AddEditProductEffect.NavigateBack)
        }
    }

    private fun isFormValid(formData: ProductFormData): Boolean {
        return formData.name.isNotBlank() &&
                formData.description.isNotBlank() &&
                formData.price.isNotBlank() &&
                formData.price.toDoubleOrNull() != null &&
                formData.stock.isNotBlank() &&
                formData.stock.toIntOrNull() != null
    }

    private fun generateProductId(): ULong {
        return System.currentTimeMillis().toULong()
    }
}