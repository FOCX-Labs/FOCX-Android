package com.focx.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.usecase.GetProductsUseCase
import com.focx.domain.usecase.SearchProductsUseCase
import com.focx.domain.usecase.GetProductByIdUseCase
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.UpdateProductUseCase
import com.focx.domain.usecase.DeleteProductUseCase
import com.focx.presentation.intent.ProductListIntent
import com.focx.presentation.state.FilterState
import com.focx.presentation.state.ProductListState
import com.focx.presentation.state.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getCurrentWalletAddressUseCase: GetCurrentWalletAddressUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProductListState(isLoading = true))
    val state: StateFlow<ProductListState> = _state.asStateFlow()

    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _effect = MutableSharedFlow<ProductListEffect>()
    val effect: SharedFlow<ProductListEffect> = _effect.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        handleIntent(ProductListIntent.LoadProducts)
        observeSearchQuery()
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .collect { query ->
                    searchProducts(query)
                }
        }
    }

    fun handleIntent(intent: ProductListIntent) {
        when (intent) {
            is ProductListIntent.LoadProducts -> loadProducts()
            is ProductListIntent.RefreshProducts -> refreshProducts()
            is ProductListIntent.LoadProductById -> loadProductById(intent.productId)
            is ProductListIntent.SearchProducts -> searchProducts(intent.query)
            is ProductListIntent.UpdateSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = intent.query)
                _searchQuery.value = intent.query
            }

            is ProductListIntent.SelectCategory -> selectCategory(intent.category)
            is ProductListIntent.SelectProduct -> selectProduct(intent.productId)
            is ProductListIntent.ShowFilter -> showFilter()
            is ProductListIntent.HideFilter -> hideFilter()
            is ProductListIntent.ToggleFilterSheet -> toggleFilterSheet()
            is ProductListIntent.ApplyFilter -> applyFilter(
                intent.minPrice,
                intent.maxPrice,
                intent.categories,
                intent.sortBy
            )

            is ProductListIntent.ApplyFilters -> applyFilters(
                intent.categories,
                intent.sortOption,
                intent.priceRange
            )

            is ProductListIntent.RemoveCategoryFilter -> removeCategoryFilter(intent.category)
            is ProductListIntent.ClearSort -> clearSort()
            is ProductListIntent.ClearFilter -> clearFilter()
            is ProductListIntent.ClearFilters -> clearFilters()
        }
    }

    private fun loadProducts(refresh: Boolean = false) {
        if (isLoading || (isLastPage && !refresh)) return

        viewModelScope.launch {
            isLoading = true
            if (refresh) {
                currentPage = 1
                isLastPage = false
                // Don't clear products immediately during refresh, wait for API response
                _state.value = _state.value.copy(isRefreshing = true)
            } else {
                _state.value = _state.value.copy(isLoading = true)
            }

            getProductsUseCase(page = currentPage, pageSize = 30, refresh = refresh)
                .catch { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                    isLoading = false
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { newProducts ->
                            if (newProducts.isEmpty()) {
                                isLastPage = true
                            } else {
                                val currentProducts = if (refresh) {
                                    // For refresh, replace products with new data from API
                                    newProducts
                                } else {
                                    // For pagination, append new products
                                    _state.value.products + newProducts
                                }
                                _state.value = _state.value.copy(
                                    products = currentProducts,
                                    filteredProducts = currentProducts
                                )
                                currentPage++
                            }
                        },
                        onFailure = { exception ->
                            _state.value = _state.value.copy(
                                error = exception.message ?: "Failed to load products"
                            )
                        }
                    )
                    isLoading = false
                    _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                }
        }
    }

    private fun refreshProducts() {
        viewModelScope.launch {
            // Check if there's an active search query
            val currentSearchQuery = _state.value.searchQuery
            if (currentSearchQuery.isNotBlank()) {
                // Refresh search results if there's a search query
                searchProducts(currentSearchQuery, refresh = true)
            } else {
                // Refresh general products if no search query
                loadProducts(refresh = true)
            }
            _effect.emit(ProductListEffect.ScrollToTop)
        }
    }

    private fun loadProductById(productId: String) {
        Log.d("ProductListViewModel", "Loading product by ID: $productId")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            getProductByIdUseCase(productId)
                .catch { exception ->
                    Log.e("ProductListViewModel", "Error loading product by ID: ${exception.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load product"
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { product ->
                            if (product != null) {
                                Log.d("ProductListViewModel", "Successfully loaded product: ${product.name}, price: ${product.price}")
                                // Add the product to both products and filteredProducts lists
                                val updatedProducts = (listOf(product) + _state.value.products).distinctBy { it.id }
                                val updatedFilteredProducts = (_state.value.filteredProducts + product).distinctBy { it.id }
                                Log.d("ProductListViewModel", "Updated products list size: ${updatedProducts.size}, filtered products size: ${updatedFilteredProducts.size}")
                                _state.value = _state.value.copy(
                                    products = updatedProducts,
                                    filteredProducts = updatedFilteredProducts,
                                    isLoading = false,
                                    error = null
                                )
                                Log.d("ProductListViewModel", "State updated with new product data")
                            } else {
                                Log.w("ProductListViewModel", "Product not found for ID: $productId")
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Product not found"
                                )
                            }
                        },
                        onFailure = { exception ->
                            Log.e("ProductListViewModel", "Failed to load product by ID: ${exception.message}")
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load product"
                            )
                        }
                    )
                }
        }
    }

    private fun searchProducts(query: String, refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                // During refresh, only set isRefreshing, don't set isSearching to avoid clearing content
                _state.value = _state.value.copy(isRefreshing = true)
            } else {
                // For new searches, set isSearching to show loading state
                _state.value = _state.value.copy(isSearching = true)
            }

            if (query.isBlank()) {
                _state.value = _state.value.copy(
                    filteredProducts = _state.value.products,
                    isSearching = false,
                    isRefreshing = false
                )
                return@launch
            }

            searchProductsUseCase(query, 1, 20) // TODO: Implement pagination
                .collect { result ->
                    result.fold(
                        onSuccess = { products ->
                            _state.value = _state.value.copy(
                                filteredProducts = products,
                                isSearching = false,
                                isRefreshing = false
                            )
                        },
                        onFailure = { exception ->
                            _state.value = _state.value.copy(
                                isSearching = false,
                                isRefreshing = false,
                                error = exception.message ?: "Search failed"
                            )
                        }
                    )
                }
        }
    }

    private fun selectCategory(category: String) {
        _state.value = _state.value.copy(selectedCategory = category)

        val filteredProducts = if (category == "All") {
            _state.value.products
        } else {
            _state.value.products.filter { it.category == category }
        }

        _state.value = _state.value.copy(filteredProducts = filteredProducts)
    }

    private fun selectProduct(productId: String) {
        viewModelScope.launch {
            _effect.emit(ProductListEffect.NavigateToProductDetail(productId))
        }
    }

    private fun showFilter() {
        _filterState.value = _filterState.value.copy(isVisible = true)
    }

    private fun hideFilter() {
        _filterState.value = _filterState.value.copy(isVisible = false)
    }

    private fun applyFilter(
        minPrice: Double,
        maxPrice: Double,
        categories: Set<String>,
        sortBy: SortOption
    ) {
        _filterState.value = _filterState.value.copy(
            minPrice = minPrice,
            maxPrice = maxPrice,
            selectedCategories = categories,
            sortBy = sortBy,
            isVisible = false
        )

        var filteredProducts = _state.value.products

        // Apply price filter
        filteredProducts = filteredProducts.filter { product ->
            val minPriceMicros = (minPrice * 1_000_000).toULong()
            val maxPriceMicros = (maxPrice * 1_000_000).toULong()
            product.price >= minPriceMicros && product.price <= maxPriceMicros
        }

        // Apply category filter
        if (categories.isNotEmpty()) {
            filteredProducts = filteredProducts.filter { product ->
                categories.contains(product.category)
            }
        }

        // Apply sorting
        filteredProducts = when (sortBy) {
            SortOption.PRICE_LOW_TO_HIGH -> filteredProducts.sortedBy { it.price }
            SortOption.PRICE_HIGH_TO_LOW -> filteredProducts.sortedByDescending { it.price }
            SortOption.NEWEST -> filteredProducts.sortedByDescending { it.id }
            SortOption.BEST_SELLING -> filteredProducts.sortedByDescending { it.salesCount }
            SortOption.LEAST_SELLING -> filteredProducts.sortedBy { it.salesCount }
            SortOption.HIGHEST_RATED -> filteredProducts.sortedByDescending { it.rating }
            SortOption.RELEVANCE -> filteredProducts
        }

        _state.value = _state.value.copy(filteredProducts = filteredProducts)
    }


    private fun clearFilter() {
        _filterState.value = FilterState()
        _state.value = _state.value.copy(
            filteredProducts = _state.value.products,
            selectedCategory = "All"
        )
    }


    private fun toggleFilterSheet() {
        _state.value = _state.value.copy(showFilterSheet = !_state.value.showFilterSheet)
    }

    private fun applyFilters(
        categories: Set<String>,
        sortOption: SortOption?,
        priceRange: Pair<Double, Double>?
    ) {
        _filterState.value = _filterState.value.copy(
            selectedCategories = categories,
            selectedSortOption = sortOption,
            priceRange = priceRange
        )

        var filteredProducts = _state.value.products

        // Apply category filter
        if (categories.isNotEmpty()) {
            filteredProducts = filteredProducts.filter { product ->
                categories.contains(product.category)
            }
        }

        // Apply price filter
        priceRange?.let { (minPrice, maxPrice) ->
            filteredProducts = filteredProducts.filter { product ->
                val minPriceMicros = (minPrice * 1_000_000).toULong()
                val maxPriceMicros = (maxPrice * 1_000_000).toULong()
                product.price >= minPriceMicros && product.price <= maxPriceMicros
            }
        }

        // Apply sorting
        sortOption?.let { sort ->
            filteredProducts = when (sort) {
                SortOption.PRICE_LOW_TO_HIGH -> filteredProducts.sortedBy { it.price }
                SortOption.PRICE_HIGH_TO_LOW -> filteredProducts.sortedByDescending { it.price }
                SortOption.NEWEST -> filteredProducts.sortedByDescending { it.id }
                SortOption.BEST_SELLING -> filteredProducts.sortedByDescending { it.salesCount }
                SortOption.LEAST_SELLING -> filteredProducts.sortedBy { it.salesCount }
                SortOption.HIGHEST_RATED -> filteredProducts.sortedByDescending { it.rating }
                SortOption.RELEVANCE -> filteredProducts
            }
        }

        _state.value = _state.value.copy(filteredProducts = filteredProducts)
    }

    private fun removeCategoryFilter(category: String) {
        val updatedCategories = _filterState.value.selectedCategories.toMutableSet()
        updatedCategories.remove(category)

        _filterState.value = _filterState.value.copy(selectedCategories = updatedCategories)

        // Reapply filters
        applyFilters(
            updatedCategories,
            _filterState.value.selectedSortOption,
            _filterState.value.priceRange
        )
    }

    private fun clearSort() {
        _filterState.value = _filterState.value.copy(selectedSortOption = null)

        // Reapply filters without sorting
        applyFilters(
            _filterState.value.selectedCategories,
            null,
            _filterState.value.priceRange
        )
    }

    private fun clearFilters() {
        _filterState.value = FilterState()
        _state.value = _state.value.copy(
            filteredProducts = _state.value.products,
            selectedCategory = "All"
        )
    }

    fun getCurrentWalletAddress(): String? {
        return getCurrentWalletAddressUseCase.execute()
    }

    fun updateProduct(product: com.focx.domain.entity.Product, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        viewModelScope.launch {
            val walletAddress = getCurrentWalletAddressUseCase.execute()
            if (walletAddress != null) {
                try {
                    updateProductUseCase(product, walletAddress, activityResultSender)
                    // Refresh products after update
                    handleIntent(ProductListIntent.RefreshProducts)
                } catch (e: Exception) {
                    _effect.emit(ProductListEffect.ShowMessage("Failed to update product: ${e.message}"))
                }
            } else {
                _effect.emit(ProductListEffect.ShowMessage("Wallet not connected"))
            }
        }
    }

    fun deleteProduct(productId: ULong, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender) {
        viewModelScope.launch {
            val walletAddress = getCurrentWalletAddressUseCase.execute()
            if (walletAddress != null) {
                try {
                    deleteProductUseCase(productId, walletAddress, activityResultSender)
                    // Refresh products after deletion
                    handleIntent(ProductListIntent.RefreshProducts)
                    _effect.emit(ProductListEffect.ShowMessage("Product deleted successfully"))
                } catch (e: Exception) {
                    _effect.emit(ProductListEffect.ShowMessage("Failed to delete product: ${e.message}"))
                }
            } else {
                _effect.emit(ProductListEffect.ShowMessage("Wallet not connected"))
            }
        }
    }
}

sealed class ProductListEffect {
    data class NavigateToProductDetail(val productId: String) : ProductListEffect()
    data class ShowMessage(val message: String) : ProductListEffect()
    object ScrollToTop : ProductListEffect()
}