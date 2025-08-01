package com.focx.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focx.domain.entity.Product
import com.focx.presentation.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerManagementScreen(
    onBackClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onEditProductClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val allProducts = remember {
        listOf(
            Product(
                id = 1UL,
                name = "iPhone 15 Pro Max  Apple",
                description = "Latest iPhone with titanium design",
                price = 1199999000UL,
                currency = "USDC",
                imageUrls = listOf(
                    "https://example.com/iphone1.jpg",
                    "https://example.com/iphone2.jpg"
                ),
                sellerId = "seller1",
                sellerName = "TechStore",
                category = "Electronics",
                stock = 25,
                salesCount = 156,
                shippingFrom = "New York",
                shippingTo = listOf("US", "CA"),
                shippingMethods = listOf("Standard", "Express"),
                rating = 4.8f,
                reviewCount = 245,
                keywords = listOf()
            )
        )
    }

    val filteredProducts = remember(searchQuery, selectedFilter) {
        allProducts.filter { product ->
            val matchesSearch = product.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Low Stock" -> product.stock < 10
                "High Sales" -> product.salesCount > 100
                "All" -> true
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Products") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Show filter options */ }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProductClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Product"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            item {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search products...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true
                )
            }

            item {
                // Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    listOf("All", "Low Stock", "High Sales").forEach { filter ->
                        FilterChip(
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            selected = selectedFilter == filter
                        )
                    }
                }
            }

            item {
                Text(
                    text = "${filteredProducts.size} products found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(filteredProducts, key = { it.id.toString() }) { product ->
                ProductManagementCard(
                    product = product,
                    onProductClick = { onProductClick(product.id.toString()) },
                    onEditClick = { onEditProductClick(product.id.toString()) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // FAB space
            }
        }
    }
}

@Composable
fun ProductManagementCard(
    product: Product,
    onProductClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProductClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Image Placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = product.name.take(2).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.medium))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$${String.format("%.2f", product.price.toDouble() / 1_000_000)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Product"
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Product Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProductStat(
                    label = "Sales",
                    value = product.salesCount.toString(),
                    color = Color(0xFF4CAF50)
                )
                ProductStat(
                    label = "Stock",
                    value = product.stock.toString(),
                    color = if (product.stock < 10) Color(0xFFFF5722) else Color(0xFF2196F3)
                )
                ProductStat(
                    label = "Rating",
                    value = "${product.rating}",
                    color = Color(0xFFFF9800)
                )
                ProductStat(
                    label = "Reviews",
                    value = product.reviewCount.toString(),
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
fun ProductStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}