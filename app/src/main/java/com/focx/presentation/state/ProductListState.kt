package com.focx.presentation.state

import com.focx.domain.entity.Product

data class ProductListState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isSearching: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val showFilterSheet: Boolean = false
)

data class ProductDetailState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val error: String? = null,
    val currentImageIndex: Int = 0,
    val quantity: Int = 1,
    val isAddingToCart: Boolean = false
)

data class FilterState(
    val isVisible: Boolean = false,
    val minPrice: Double = 0.0,
    val maxPrice: Double = 10000.0,
    val selectedCategories: Set<String> = emptySet(),
    val sortBy: SortOption = SortOption.RELEVANCE,
    val selectedSortOption: SortOption? = null,
    val priceRange: Pair<Double, Double>? = null
)

enum class SortOption(val displayName: String) {
    RELEVANCE("Relevance"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low"),
    NEWEST("Newest"),
    BEST_SELLING("Best Selling"),
    LEAST_SELLING("Least Selling"),
    HIGHEST_RATED("Highest Rated")
}