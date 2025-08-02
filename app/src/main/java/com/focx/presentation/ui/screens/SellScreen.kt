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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderManagementStatus
import com.focx.domain.entity.Product
import com.focx.presentation.ui.theme.FocxTheme
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.viewmodel.SellViewModel
import com.focx.presentation.viewmodel.SellerRegistrationViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.delay
import com.focx.utils.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    activityResultSender: ActivityResultSender,
    onNavigateToSellerDashboard: () -> Unit = {},
    onNavigateToSellerRegistration: () -> Unit = {},
    onNavigateToAddProduct: () -> Unit = {},
    onNavigateToOrderDetail: (String) -> Unit = {},
    viewModel: SellViewModel = hiltViewModel(),
    registrationViewModel: SellerRegistrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val registrationState by registrationViewModel.uiState.collectAsStateWithLifecycle()

    // If the user is not registered as a seller, show the registration page
    if (!registrationState.isRegistered) {
        SellerRegistrationScreen(
            activityResultSender = activityResultSender,
            onRegistrationSuccess = {
                // Update registration status and cache
                registrationViewModel.setRegistered(true)
            }
        )
        return
    }

    // Load data when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.loadSellerData()
    }
    
    // Refresh data when screen is re-entered (using a key that changes on navigation)
    LaunchedEffect(Unit) {
        Log.d("SellScreen", "Screen entered, refreshing data...")
        viewModel.refreshData()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (registrationState.isRegistered) {
                FloatingActionButton(
                    onClick = onNavigateToAddProduct,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Product"
                    )
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = if (uiState.isDataCached) "Refreshing data..." else "Loading data...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Seller Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Show cache status and refresh button
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.refreshData() },
                                    enabled = !uiState.isLoading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh data",
                                        tint = if (uiState.isLoading) 
                                            MaterialTheme.colorScheme.onSurfaceVariant 
                                        else 
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Statistics Cards
                    uiState.sellerStats?.let { stats ->
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StatCard(
                                        title = "Orders",
                                        value = stats.orderCounts.toString(),
                                        icon = Icons.Default.ShoppingCart,
                                        color = Color(0xFF2196F3),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        title = "Products",
                                        value = stats.productCounts.toString(),
                                        icon = Icons.Default.Inventory,
                                        color = Color(0xFF9C27B0),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // My Products Section
                    item {
                        SectionHeader(
                            title = "My Products",
                            actionText = "View All",
                            onActionClick = onNavigateToSellerDashboard
                        )
                    }

                    items(uiState.myProducts) { product ->
                        ProductSellerCard(
                            product = product,
                            onProductClick = { /* Navigate to product detail */ },
                            onEditClick = { /* Navigate to edit product */ },
                            onDeleteClick = { /* Show delete confirmation */ }
                        )
                    }

                    // Recent Orders Section
                    item {
                        SectionHeader(
                            title = "Recent Orders",
                            actionText = "View All",
                            onActionClick = { /* Navigate to all orders */ }
                        )
                    }

                    items(uiState.recentOrders) { order ->
                        OrderCard(
                            order = order,
                            onOrderClick = { onNavigateToOrderDetail(order.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // FAB space
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onActionClick) {
            Text(actionText)
        }
    }
}

@Composable
fun ProductSellerCard(
    product: Product,
    onProductClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .clickable { onProductClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = product.imageUrls.firstOrNull(),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "${product.price} ${product.currency}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Stock: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun SellScreenPreview() {
    FocxTheme {
        // Note: Preview doesn't support ActivityResultSender, so this won't work in preview
        // SellScreen requires ActivityResultSender parameter
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun StatCardPreview() {
    FocxTheme {
        StatCard(
            title = "Total Sales",
            value = "$12,345",
            icon = Icons.Default.AttachMoney,
            color = Color(0xFF4CAF50)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun SectionHeaderPreview() {
    FocxTheme {
        SectionHeader(
            title = "My Products",
            actionText = "View All",
            onActionClick = { }
        )
    }
}

@Composable
fun OrderCard(
    order: Order,
    onOrderClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOrderClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.items[0].productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Order: ${order.id}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Buyer: ${order.buyerId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${order.totalAmount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                                        text = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US)
                    .format(java.util.Date(order.orderDate * 1000)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Surface(
                color = when (order.status) {
                    OrderManagementStatus.Delivered -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    OrderManagementStatus.Shipped -> Color(0xFF2196F3).copy(alpha = 0.1f)
                    OrderManagementStatus.Pending -> Color(0xFFFF9800).copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = order.status.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (order.status) {
                        OrderManagementStatus.Delivered -> Color(0xFF4CAF50)
                        OrderManagementStatus.Shipped -> Color(0xFF2196F3)
                        OrderManagementStatus.Pending -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}