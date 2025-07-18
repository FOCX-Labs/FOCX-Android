package com.focx.presentation.state

import com.focx.domain.entity.Product

data class SearchState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchHistory: List<String> = emptyList(),
    val trendingSearches: List<String> = emptyList(),
    val searchSuggestions: List<String> = emptyList()
)