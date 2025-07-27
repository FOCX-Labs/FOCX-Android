package com.focx.presentation.intent

import com.focx.presentation.ui.screens.ProductFormData

sealed class AddEditProductIntent {
    data class UpdateFormData(val formData: ProductFormData) : AddEditProductIntent()
    data class SaveProduct(val activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) : AddEditProductIntent()
    object NavigateBack : AddEditProductIntent()
}