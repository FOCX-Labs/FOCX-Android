package com.focx.presentation.intent

import com.focx.presentation.state.SortOption

sealed class ProductListIntent {
    object LoadProducts : ProductListIntent()
    object RefreshProducts : ProductListIntent()
    data class SearchProducts(val query: String) : ProductListIntent()
    data class UpdateSearchQuery(val query: String) : ProductListIntent()
    data class SelectCategory(val category: String) : ProductListIntent()
    data class SelectProduct(val productId: String) : ProductListIntent()
    object ShowFilter : ProductListIntent()
    object HideFilter : ProductListIntent()
    object ToggleFilterSheet : ProductListIntent()
    data class ApplyFilter(
        val minPrice: Double,
        val maxPrice: Double,
        val categories: Set<String>,
        val sortBy: SortOption
    ) : ProductListIntent()

    data class ApplyFilters(
        val categories: Set<String>,
        val sortOption: SortOption?,
        val priceRange: Pair<Double, Double>?
    ) : ProductListIntent()

    data class RemoveCategoryFilter(val category: String) : ProductListIntent()
    object ClearSort : ProductListIntent()
    object ClearFilter : ProductListIntent()
    object ClearFilters : ProductListIntent()
}

sealed class ProductDetailIntent {
    data class LoadProduct(val productId: String) : ProductDetailIntent()
    data class ChangeImageIndex(val index: Int) : ProductDetailIntent()
    data class UpdateQuantity(val quantity: Int) : ProductDetailIntent()
    object BuyNow : ProductDetailIntent()
    object NavigateBack : ProductDetailIntent()
}