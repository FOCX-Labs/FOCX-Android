package com.focx.presentation.state

import com.focx.domain.entity.Product
import com.focx.presentation.ui.screens.ProductFormData

data class AddEditProductState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val formData: ProductFormData = ProductFormData(),
    val originalProduct: Product? = null,
    val error: String? = null
)