package com.focx.presentation.intent

sealed class SearchIntent {
    data class UpdateSearchQuery(val query: String) : SearchIntent()
    data class SearchProducts(val query: String) : SearchIntent()
    object ClearSearch : SearchIntent()
}