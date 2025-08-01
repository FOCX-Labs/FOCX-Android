package com.focx.presentation.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focx.domain.entity.Product
import com.focx.presentation.intent.ProductListIntent
import com.focx.presentation.state.SortOption
import com.focx.presentation.ui.components.EmptyState
import com.focx.presentation.ui.components.ErrorState
import com.focx.presentation.ui.components.LoadingSize
import com.focx.presentation.ui.components.ProductCard
import com.focx.presentation.ui.components.ShimmerProductGrid
import com.focx.presentation.ui.components.TechButton
import com.focx.presentation.ui.components.TechButtonStyle
import com.focx.presentation.ui.components.TechLoadingIndicator
import com.focx.presentation.ui.theme.FocxTheme
import com.focx.presentation.ui.theme.OnSurface
import com.focx.presentation.ui.theme.OnSurfaceVariant
import com.focx.presentation.ui.theme.Primary
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.ui.theme.SurfaceDark
import com.focx.presentation.viewmodel.ProductListEffect
import com.focx.presentation.viewmodel.ProductListViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ProductListScreen(
    onProductClick: (Product) -> Unit, modifier: Modifier = Modifier, viewModel: ProductListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val searchQuery = state.searchQuery
    val lazyGridState = rememberLazyGridState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing, onRefresh = { viewModel.handleIntent(ProductListIntent.RefreshProducts) })

    // Listen for scroll to top effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProductListEffect.ScrollToTop -> {
                    lazyGridState.animateScrollToItem(0)
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(lazyGridState, state.searchQuery) {
        snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect {
            // Only trigger load more when not searching
            if (it != null && state.searchQuery.isBlank() && it >= state.products.size - 1 && !state.isLoading && !state.isRefreshing) {
                viewModel.handleIntent(ProductListIntent.LoadProducts)
            }
        }
    }

    // Scroll to top when sort option changes
    LaunchedEffect(filterState.selectedSortOption) {
        if (filterState.selectedSortOption != null) {
            lazyGridState.scrollToItem(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBarWithFilters(
                searchQuery = state.searchQuery, 
                onSearch = { query ->
                    viewModel.handleIntent(ProductListIntent.UpdateSearchQuery(query))
                }, 
                onFilterClick = {
                    viewModel.handleIntent(ProductListIntent.ToggleFilterSheet)
                }, 
                onSortChange = { sortOption ->
                    viewModel.handleIntent(
                        ProductListIntent.ApplyFilters(
                            categories = filterState.selectedCategories,
                            sortOption = sortOption,
                            priceRange = filterState.priceRange
                        )
                    )
                }, 
                currentSortOption = filterState.selectedSortOption,
                isSearching = state.isSearching
            )

            Box(modifier = Modifier.weight(1f)) {
                // Content
                Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
                    when {
                        // 显示搜索loading状态
                        state.isSearching && state.searchQuery.isNotBlank() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    TechLoadingIndicator(size = LoadingSize.LARGE)
                                    Spacer(modifier = Modifier.height(Spacing.medium))
                                    Text(
                                        text = "搜索中...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        (state.isLoading || state.isRefreshing) && state.products.isEmpty() -> {
                            ShimmerProductGrid(
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        state.error != null && state.products.isEmpty() && !state.isRefreshing -> {
                            ErrorState(
                                title = "Failed to load products", subtitle = state.error, onActionClick = {
                                    viewModel.handleIntent(ProductListIntent.LoadProducts)
                                }, modifier = Modifier.fillMaxSize()
                            )
                        }

                        state.products.isEmpty() && !state.isRefreshing && !state.isSearching -> {
                            EmptyState(
                                title = "No products found",
                                subtitle = "Try adjusting your search or filters",
                                icon = androidx.compose.material.icons.Icons.Default.SearchOff,
                                actionText = "Browse All",
                                onActionClick = {
                                    viewModel.handleIntent(ProductListIntent.ClearFilters)
                                    viewModel.handleIntent(ProductListIntent.LoadProducts)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            ProductGrid(
                                products = if (filterState.selectedSortOption != SortOption.RELEVANCE) state.filteredProducts else state.products,
                                onProductClick = onProductClick,
                                onFavoriteClick = { product ->
                                    // TODO: Implement favorite functionality
                                },
                                isLoading = state.isLoading,
                                modifier = Modifier.fillMaxSize(),
                                state = lazyGridState
                            )
                        }
                    }

                    // Pull to refresh indicator
                    PullRefreshIndicator(
                        refreshing = state.isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
                if (state.showFilterSheet) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        onDismissRequest = { viewModel.handleIntent(ProductListIntent.ToggleFilterSheet) }) {
                        ProductFilterCard(filterState = filterState, onDismiss = {
                            viewModel.handleIntent(ProductListIntent.ToggleFilterSheet)
                        }, onApplyFilters = { priceRange ->
                            viewModel.handleIntent(
                                ProductListIntent.ApplyFilters(
                                    categories = filterState.selectedCategories, // Keep existing categories
                                    sortOption = filterState.selectedSortOption, // Keep existing sort option
                                    priceRange = priceRange
                                )
                            )
                            viewModel.handleIntent(ProductListIntent.ToggleFilterSheet)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGrid(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    onFavoriteClick: (Product) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    state: androidx.compose.foundation.lazy.grid.LazyGridState = rememberLazyGridState()
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Spacing.medium, end = Spacing.medium, top = Spacing.small, bottom = Spacing.extraLarge
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        items(
            items = products, key = { it.id.toString() }) { product ->
            ProductCard(
                product = product, onClick = onProductClick, onFavoriteClick = onFavoriteClick
            )
        }

        // Loading indicator at the bottom
        if (isLoading && products.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium), contentAlignment = Alignment.Center
                ) {
                    TechLoadingIndicator(size = LoadingSize.MEDIUM)
                }
            }
        }
    }
}

@Composable
fun SearchBarWithFilters(
    searchQuery: String = "",
    onSearch: (String) -> Unit,
    onFilterClick: () -> Unit,
    onSortChange: (SortOption?) -> Unit = {},
    currentSortOption: SortOption? = null,
    isSearching: Boolean = false
) {
    var localSearchQuery by remember(searchQuery) { mutableStateOf(searchQuery) }
    var selectedSort by remember(currentSortOption) {
        mutableStateOf(
            when (currentSortOption) {
                SortOption.BEST_SELLING, SortOption.LEAST_SELLING -> "Sales"
                SortOption.PRICE_LOW_TO_HIGH, SortOption.PRICE_HIGH_TO_LOW -> "Price"
                else -> "Recommended"
            }
        )
    }
    var priceAscending by remember(currentSortOption) {
        mutableStateOf(currentSortOption != SortOption.PRICE_HIGH_TO_LOW)
    }
    var salesDescending by remember(currentSortOption) {
        mutableStateOf(currentSortOption == SortOption.BEST_SELLING)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.medium)
    ) {
        // Search Box
        OutlinedTextField(
            value = localSearchQuery,
            onValueChange = {
                localSearchQuery = it
                onSearch(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Products") },
            leadingIcon = { 
                if (isSearching) {
                    TechLoadingIndicator(size = LoadingSize.SMALL)
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Filter/Sort Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                selectedSort = "Recommended"
                onSortChange(null)
            }) {
                Text("Recommended", color = if (selectedSort == "Recommended") Primary else OnSurfaceVariant)
            }
            TextButton(onClick = {
                selectedSort = "Sales"
                salesDescending = !salesDescending
                onSortChange(if (salesDescending) SortOption.BEST_SELLING else SortOption.LEAST_SELLING)
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sales", color = if (selectedSort == "Sales") Primary else OnSurfaceVariant)
                    Icon(
                        imageVector = if (salesDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = "Sort by sales",
                        tint = if (selectedSort == "Sales") Primary else OnSurfaceVariant
                    )
                }
            }
            TextButton(onClick = {
                selectedSort = "Price"
                priceAscending = !priceAscending
                onSortChange(if (priceAscending) SortOption.PRICE_LOW_TO_HIGH else SortOption.PRICE_HIGH_TO_LOW)
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Price", color = if (selectedSort == "Price") Primary else OnSurfaceVariant)
                    Icon(
                        imageVector = if (priceAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Sort by price",
                        tint = if (selectedSort == "Price") Primary else OnSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onFilterClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Filter", color = OnSurfaceVariant)
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = OnSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFilterCard(
    filterState: com.focx.presentation.state.FilterState,
    onDismiss: () -> Unit,
    onApplyFilters: (Pair<Double, Double>?) -> Unit
) {
    var priceRange by remember { mutableStateOf(filterState.priceRange) }
    var minPrice by remember { mutableStateOf(filterState.priceRange?.first?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(filterState.priceRange?.second?.toString() ?: "") }

    LaunchedEffect(minPrice, maxPrice) {
        val min = minPrice.toDoubleOrNull()
        val max = maxPrice.toDoubleOrNull()
        if (min != null && max != null && min <= max) {
            priceRange = min to max
        } else {
            priceRange = null
        }
    }

    Surface(
        modifier = Modifier.width(300.dp),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        color = SurfaceDark,
        contentColor = OnSurface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Text(
                text = "Filter Products", style = MaterialTheme.typography.headlineSmall, color = OnSurface
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            // Price Range
            Text(
                text = "Price Range", style = MaterialTheme.typography.titleMedium, color = OnSurface
            )

            Spacer(modifier = Modifier.height(Spacing.medium))



            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = { minPrice = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Min Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text(" - ", modifier = Modifier.padding(horizontal = Spacing.small))
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Max Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Price presets
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                PricePresetChip(
                    range = "0-1999",
                    label = "Economical",
                    selected = minPrice == "0" && maxPrice == "1999",
                    onClick = { minPrice = "0"; maxPrice = "1999" },
                    modifier = Modifier.weight(1f)
                )
                PricePresetChip(
                    range = "2000-3999",
                    label = "Mid-range",
                    selected = minPrice == "2000" && maxPrice == "3999",
                    onClick = { minPrice = "2000"; maxPrice = "3999" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.small))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                PricePresetChip(
                    range = "4000-7999",
                    label = "Premium",
                    selected = minPrice == "4000" && maxPrice == "7999",
                    onClick = { minPrice = "4000"; maxPrice = "7999" },
                    modifier = Modifier.weight(1f)
                )
                PricePresetChip(
                    range = "8000-99999",
                    label = "Luxury",
                    selected = minPrice == "8000" && maxPrice == "99999",
                    onClick = { minPrice = "8000"; maxPrice = "99999" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                TechButton(
                    text = "Clear", onClick = {
                        minPrice = ""
                        maxPrice = ""
                    }, modifier = Modifier.weight(1f), style = TechButtonStyle.OUTLINE
                )
                TechButton(
                    text = "Apply", onClick = {
                        onApplyFilters(priceRange)
                    }, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricePresetChip(
    range: String, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(
            containerColor = if (selected) Primary.copy(alpha = 0.1f) else SurfaceDark
        ), border = BorderStroke(1.dp, if (selected) Primary else Color.Gray), modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.small, horizontal = Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(range, color = if (selected) Primary else Color.White)
            Text(label, style = MaterialTheme.typography.bodySmall, color = if (selected) Primary else Color.Gray)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun ProductFilterCardPreview() {
    FocxTheme {
        ProductFilterCard(
            filterState = com.focx.presentation.state.FilterState(),
            onDismiss = { },
            onApplyFilters = { priceRange: Pair<Double, Double>? -> })
    }
}