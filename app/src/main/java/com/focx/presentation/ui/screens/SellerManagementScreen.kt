package com.focx.presentation.ui.screens

import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.focx.domain.entity.Product
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.viewmodel.SellerManagementViewModel
import com.focx.utils.Log
import com.focx.utils.ShopUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerManagementScreen(
    onBackClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onEditProductClick: (String) -> Unit,
    merchantAddress: String?,
    navController: NavController,
    viewModel: SellerManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    
    // Get current back stack entry to access saved state
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    // State to track if we need to refresh
    var shouldRefresh by remember { mutableStateOf(false) }

    // Debug log for merchant address
    LaunchedEffect(merchantAddress) {
        Log.d("SellerManagementScreen", "merchantAddress: $merchantAddress")
    }

    // Load data when merchant address is available
    LaunchedEffect(merchantAddress) {
        merchantAddress?.let { address ->
            Log.d("SellerManagementScreen", "Loading merchant products for address: $address")
            viewModel.loadMerchantProducts(address)
        } ?: run {
            Log.w("SellerManagementScreen", "merchantAddress is null, cannot load products")
        }
    }

    // Check for refresh flag and trigger refresh
    LaunchedEffect(Unit) {
        Log.d("SellerManagementScreen", "Checking for refresh flag on screen activation")
        kotlinx.coroutines.delay(300) // Give navigation state time to update
        
        val refreshFlag = currentBackStackEntry?.savedStateHandle?.get<Boolean>("refresh_products") ?: false
        Log.d("SellerManagementScreen", "Refresh flag found: $refreshFlag, merchantAddress: $merchantAddress")
        
        if (refreshFlag && merchantAddress != null) {
            Log.d("SellerManagementScreen", "Triggering refresh due to flag")
            viewModel.refresh(merchantAddress)
            currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_products")
            Log.d("SellerManagementScreen", "Refresh completed and flag cleared")
        }
    }

    // Additional check when currentBackStackEntry changes
    LaunchedEffect(currentBackStackEntry) {
        Log.d("SellerManagementScreen", "currentBackStackEntry changed: ${currentBackStackEntry?.destination?.route}")
        
        val refreshFlag = currentBackStackEntry?.savedStateHandle?.get<Boolean>("refresh_products") ?: false
        Log.d("SellerManagementScreen", "BackStackEntry change - refresh flag: $refreshFlag, merchantAddress: $merchantAddress")
        
        if (refreshFlag && merchantAddress != null) {
            Log.d("SellerManagementScreen", "Triggering refresh due to backStackEntry change")
            viewModel.refresh(merchantAddress)
            currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_products")
            Log.d("SellerManagementScreen", "Refresh completed and flag cleared from backStackEntry change")
        }
    }

    // Show toast when there's an error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    val filteredProducts = remember(searchQuery, selectedFilter, uiState.products) {
        uiState.products.filter { product ->
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
//                    IconButton(onClick = { /* Show filter options */ }) {
//                        Icon(
//                            imageVector = Icons.Default.FilterList,
//                            contentDescription = "Filter"
//                        )
//                    }
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
        if (merchantAddress == null) {
            // Show wallet connection required message
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Text(
                        text = "Wallet Connection Required",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Please connect your wallet to view your products",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                item {
                    Text(
                        text = "${filteredProducts.size} products found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (filteredProducts.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No products found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredProducts, key = { it.id.toString() }) { product ->
                        ProductManagementCard(
                            product = product,
                            onProductClick = { onProductClick(product.id.toString()) },
                            onEditClick = { onEditProductClick(product.id.toString()) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp)) // FAB space
                }
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
                // Product Image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (product.imageUrls.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(product.imageUrls.first())
                                .crossfade(true)
                                .build(),
                            contentDescription = "Product image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Fallback to placeholder
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
                        text = "$${ShopUtils.getPriceShow(product.price)}",
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

//                IconButton(onClick = onEditClick) {
//                    Icon(
//                        imageVector = Icons.Default.Edit,
//                        contentDescription = "Edit Product"
//                    )
//                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Product Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
//                ProductStat(
//                    label = "Sales",
//                    value = product.salesCount.toString(),
//                    color = Color(0xFF4CAF50)
//                )
                ProductStat(
                    label = "Stock",
                    value = product.stock.toString(),
                    color = if (product.stock < 10) Color(0xFFFF5722) else Color(0xFF2196F3)
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