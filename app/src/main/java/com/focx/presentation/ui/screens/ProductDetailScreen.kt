package com.focx.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.focx.domain.entity.Product
import com.focx.domain.entity.UserAddress
import com.focx.presentation.ui.components.CardStyle
import com.focx.presentation.ui.components.TechButton
import com.focx.presentation.ui.components.TechButtonSize
import com.focx.presentation.ui.components.TechButtonStyle
import com.focx.presentation.ui.components.TechCard
import com.focx.presentation.ui.components.TechIconButton
import com.focx.presentation.ui.components.TechLoadingIndicator
import com.focx.presentation.ui.components.LoadingSize
import com.focx.presentation.ui.theme.Error
import com.focx.presentation.ui.theme.OnSurface
import com.focx.presentation.ui.theme.OnSurfaceVariant
import com.focx.presentation.ui.theme.Primary
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.ui.theme.Success
import com.focx.presentation.ui.theme.SurfaceDark
import com.focx.presentation.viewmodel.ProductListViewModel
import com.focx.presentation.viewmodel.ProfileViewModel
import com.focx.presentation.intent.ProductListIntent
import com.focx.presentation.viewmodel.ProductListEffect
import com.focx.utils.ShopUtils
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    isEditMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onBuyProduct: (Product, Int, UserAddress?, String, com.solana.mobilewalletadapter.clientlib.ActivityResultSender) -> Unit,
    onEditProduct: (String) -> Unit,
    activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isFavorite by remember { mutableStateOf(false) }
    var selectedQuantity by remember { mutableStateOf(1) }
    var showBuyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Get current back stack entry to access saved state
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    // Load user addresses when screen is displayed
    LaunchedEffect(Unit) {
        profileViewModel.loadUserAddressesOnly()
    }

    // Check for refresh flag and reload product data
    LaunchedEffect(currentBackStackEntry) {
        val refreshFlag = currentBackStackEntry?.savedStateHandle?.get<Boolean>("refresh_product_detail") ?: false
        if (refreshFlag) {
            Log.d("ProductDetailScreen", "Refreshing product detail due to navigation flag")
            // Reload the specific product
            viewModel.handleIntent(ProductListIntent.LoadProductById(productId))
            // Don't clear the flag immediately, let the product loading logic handle it
            Log.d("ProductDetailScreen", "Product detail refresh initiated")
        }
    }

    // Show toast when there's an error
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            Toast.makeText(context, "Failed to load product: $error", Toast.LENGTH_LONG).show()
        }
    }

    // Listen for effects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProductListEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                    // If the message indicates successful deletion, navigate back
                    if (effect.message.contains("deleted successfully", ignoreCase = true)) {
                        onNavigateBack()
                    }
                }
                else -> {}
            }
        }
    }

    val productIdULong = productId.toULongOrNull()
    if (productIdULong == null) {
        android.util.Log.e("ProductDetailScreen", "Invalid product ID: $productId")
        return
    }
    
    // Get product from state, but force reload if refresh flag was set
    val refreshFlagWasSet = currentBackStackEntry?.savedStateHandle?.get<Boolean>("refresh_product_detail") ?: false
    var product = if (!refreshFlagWasSet) {
        state.products.find { it.id == productIdULong } 
            ?: state.filteredProducts.find { it.id == productIdULong }
    } else {
        null // Force reload when refresh flag is set
    }
    
    // Debug log for product data
    LaunchedEffect(product) {
        Log.d("ProductDetailScreen", "Product data updated: ${product?.name}, price: ${product?.price}")
    }
    
    // If product not found locally or refresh flag was set, try to load it
    if (product == null) {
        LaunchedEffect(productIdULong, refreshFlagWasSet) {
            Log.d("ProductDetailScreen", "Loading product by ID: $productIdULong, refresh flag was set: $refreshFlagWasSet")
            viewModel.handleIntent(ProductListIntent.LoadProductById(productIdULong.toString()))
        }
        
        // Clear refresh flag after initiating the load
        if (refreshFlagWasSet) {
            currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_product_detail")
            Log.d("ProductDetailScreen", "Refresh flag cleared after initiating load")
        }
        
        // Show error state if there's an error
        if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Failed to load product",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Text(
                        text = state.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.large))
                    TechButton(
                        text = "Retry",
                        onClick = {
                            viewModel.handleIntent(ProductListIntent.LoadProductById(productIdULong.toString()))
                        },
                        style = TechButtonStyle.PRIMARY
                    )
                }
            }
            return
        }
        
        // Show loading state while fetching product
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TechLoadingIndicator(size = LoadingSize.LARGE)
        }
        return
    }
    
    // Re-check for product after loading, always get the latest from state
    val currentProduct = state.products.find { it.id == productIdULong } 
        ?: state.filteredProducts.find { it.id == productIdULong }
        ?: product
    
    // Check if current user is the seller of this product
    val currentWalletAddress = viewModel.getCurrentWalletAddress()
    val isCurrentUserSeller = currentProduct?.let { product ->
        currentWalletAddress == product.sellerId
    } ?: false
    
    if (currentProduct == null) {
        // Show error state if product still not found
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Product not found",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
                Text(
                    text = "The product you're looking for doesn't exist or has been removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.large))
                TechButton(
                    text = "Go Back",
                    onClick = onNavigateBack,
                    style = TechButtonStyle.PRIMARY
                )
            }
        }
        return
    }

    val productImages = currentProduct.imageUrls

    val pagerState = rememberPagerState(pageCount = { productImages.size })

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { },
            navigationIcon = {
                TechIconButton(
                    icon = Icons.Default.ArrowBack,
                    onClick = onNavigateBack,
                    style = TechButtonStyle.GHOST
                )
            },
            actions = {
//                TechIconButton(
//                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
//                    onClick = { isFavorite = !isFavorite },
//                    style = TechButtonStyle.GHOST
//                )
//                TechIconButton(
//                    icon = Icons.Default.Share,
//                    onClick = { /* TODO: Implement share */ },
//                    style = TechButtonStyle.GHOST
//                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = OnSurface
            )
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            // Product Images
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = productImages[page],
                            contentDescription = "Product image ${page + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Image indicators
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(productImages.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (index == pagerState.currentPage) Primary
                                        else OnSurfaceVariant.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            // Product Info
            item {
                Column(
                    modifier = Modifier.padding(Spacing.medium)
                ) {
                    // Product Name and Category
                    Text(
                        text = currentProduct.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )

//                    Spacer(modifier = Modifier.height(Spacing.small))
//
//                    TechBadge(
//                        text = product.category,
//                        style = BadgeStyle.SOFT
//                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // Rating and Reviews
                    // Row(
                    //     verticalAlignment = Alignment.CenterVertically
                    // ) {
                    //     repeat(5) { index ->
                    //         Icon(
                    //             imageVector = Icons.Default.Star,
                    //             contentDescription = null,
                    //             tint = if (index < product.rating.toInt()) Primary else OnSurfaceVariant,
                    //             modifier = Modifier.size(16.dp)
                    //         )
                    //     }
                    //     Spacer(modifier = Modifier.width(Spacing.small))
                    //     Text(
                    //         text = "${product.rating} (${product.reviewCount} reviews)",
                    //         style = MaterialTheme.typography.bodyMedium,
                    //         color = OnSurfaceVariant
                    //     )
                    // }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // Price
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$${ShopUtils.getPriceShow(currentProduct.price)}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )

                        // Note: Original price and discount features can be added to Product entity if needed
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // Stock Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (currentProduct.stock > 0) Success else Error)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = if (currentProduct.stock > 0) "In Stock (${currentProduct.stock} left)" else "Out of Stock",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentProduct.stock > 0) Success else Error
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Seller Info
                    Row(
                        modifier = Modifier.padding(Spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sold by ${currentProduct.sellerName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Specifications
//                    Text(
//                        text = "Specifications",
//                        style = MaterialTheme.typography.titleLarge,
//                        color = OnSurface,
//                        fontWeight = FontWeight.Bold
//                    )
//
//                    Spacer(modifier = Modifier.height(Spacing.medium))
//
//                    val specifications = currentProduct.specifications.toList()
//
//                    specifications.forEach { (key, value) ->
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = Spacing.small)
//                        ) {
//                            TextView(
//                                text = key,
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = OnSurfaceVariant,
//                                modifier = Modifier.weight(1f)
//                            )
//                            Text(
//                                text = value,
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = OnSurface,
//                                fontWeight = FontWeight.Medium
//                            )
//                        }
//                        if (specifications.last() != key to value) {
//                            HorizontalDivider(
//                                color = OnSurfaceVariant.copy(alpha = 0.1f),
//                                thickness = 1.dp
//                            )
//                        }
//                    }

                    // Shipping Info
                    Text(
                        text = "Shipping Information",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    val shippingInfo = listOf(
                        "Origin" to currentProduct.shippingFrom,
                        "Shipping Range" to currentProduct.shippingTo.joinToString(),
                        "Logistics" to currentProduct.shippingMethods.joinToString()
                    )

                    shippingInfo.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.small)
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (shippingInfo.last() != key to value) {
                            HorizontalDivider(
                                color = OnSurfaceVariant.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Description
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    Text(
                        text = currentProduct.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurface,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))
                }
            }
        }

        // Bottom Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceDark,
            shadowElevation = 8.dp
        ) {
            if (isEditMode && currentProduct != null) {
                // Edit mode actions - Edit and Delete buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TechButton(
                        text = "Edit",
                        onClick = { onEditProduct(productId) },
                        style = TechButtonStyle.OUTLINE,
                        size = TechButtonSize.MEDIUM,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TechButton(
                        text = "Delete",
                        onClick = { showDeleteDialog = true },
                        style = TechButtonStyle.PRIMARY,
                        size = TechButtonSize.MEDIUM,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (currentProduct != null) {
                // Buyer actions - Quantity selector and Buy button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TechIconButton(
                            icon = androidx.compose.material.icons.Icons.Default.Remove,
                            onClick = { if (selectedQuantity > 1) selectedQuantity-- },
                            style = TechButtonStyle.OUTLINE,
                            enabled = selectedQuantity > 1
                        )

                        Text(
                            text = selectedQuantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface,
                            modifier = Modifier.padding(horizontal = Spacing.medium)
                        )

                        TechIconButton(
                            icon = androidx.compose.material.icons.Icons.Default.Add,
                            onClick = { if (selectedQuantity < currentProduct.stock) selectedQuantity++ },
                            style = TechButtonStyle.OUTLINE,
                            enabled = selectedQuantity < currentProduct.stock
                        )
                    }

                    // Buy Now Button
                    TechButton(
                        text = "Buy Now",
                        onClick = { showBuyDialog = true },
                        style = TechButtonStyle.PRIMARY,
                        size = TechButtonSize.MEDIUM,
                        enabled = currentProduct.stock > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Buy Dialog - only show for buyers
    if (showBuyDialog && !isEditMode && currentProduct != null) {
        BottomBuyDialog(
            product = currentProduct,
            quantity = selectedQuantity,
            addresses = profileState.userAddresses,
            onQuantityChange = { newQuantity ->
                selectedQuantity = newQuantity
            },
            onConfirmBuy = { selectedAddress, orderNote ->
                onBuyProduct(currentProduct, selectedQuantity, selectedAddress, orderNote, activityResultSender)
                showBuyDialog = false
            },
            onDismiss = {
                showBuyDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && currentProduct != null) {
        DeleteConfirmationDialog(
            productName = currentProduct.name,
            onConfirmDelete = {
                viewModel.deleteProduct(currentProduct.id, activityResultSender)
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    productName: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        TechCard(
            style = CardStyle.ELEVATED,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Delete Product",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                Text(
                    text = "Are you sure you want to delete \"$productName\"?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    TechButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        style = TechButtonStyle.OUTLINE,
                        size = TechButtonSize.MEDIUM,
                        modifier = Modifier.weight(1f)
                    )
                    TechButton(
                        text = "Delete",
                        onClick = onConfirmDelete,
                        style = TechButtonStyle.PRIMARY,
                        size = TechButtonSize.MEDIUM,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomBuyDialog(
    product: Product,
    quantity: Int,
    addresses: List<UserAddress>,
    onQuantityChange: (Int) -> Unit,
    onConfirmBuy: (UserAddress?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAddress by remember(addresses) { 
        mutableStateOf(addresses.find { it.isDefault } ?: addresses.firstOrNull()) 
    }
    var orderNote by remember { mutableStateOf("") }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            TechCard(
                style = CardStyle.ELEVATED,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.extraSmall)
                ) {
                    // Handle bar
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(OnSurfaceVariant.copy(alpha = 0.3f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Dialog Title
                    Text(
                        text = "Purchase",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Product Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = product.imageUrls.firstOrNull(),
                            contentDescription = product.name,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.width(Spacing.medium))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$${ShopUtils.getPriceShow(product.price)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Quantity Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quantity:",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TechIconButton(
                                icon = Icons.Default.Remove,
                                onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                                style = TechButtonStyle.OUTLINE,
                                size = TechButtonSize.SMALL,
                                enabled = quantity > 1
                            )

                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface,
                                modifier = Modifier.padding(horizontal = Spacing.medium)
                            )

                            TechIconButton(
                                icon = Icons.Default.Add,
                                onClick = { if (quantity < product.stock) onQuantityChange(quantity + 1) },
                                style = TechButtonStyle.OUTLINE,
                                size = TechButtonSize.SMALL,
                                enabled = quantity < product.stock
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Address Selection
                    Text(
                        text = "Delivery Address:",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(Spacing.small))

                    if (addresses.isEmpty()) {
                        TechCard(
                            style = CardStyle.OUTLINED,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.medium),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No delivery addresses found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.small))
                                Text(
                                    text = "Please add an address in your profile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    } else {
                        TechCard(
                            style = CardStyle.OUTLINED,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.medium)
                            ) {
                                addresses.forEach { address ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Spacing.small),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedAddress == address,
                                            onClick = { selectedAddress = address },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.small))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = address.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = OnSurface,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (address.isDefault) {
                                                    Spacer(modifier = Modifier.width(Spacing.small))
                                                    Surface(
                                                        color = Primary.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Default",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Primary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${address.recipientName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant
                                            )
                                            Text(
                                                text = "${address.addressLine1}${if (!address.addressLine2.isNullOrEmpty()) ", ${address.addressLine2}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant
                                            )
                                            Text(
                                                text = "${address.city}, ${address.state} ${address.postalCode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Order Note
                    Text(
                        text = "Order Note (Optional):",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(Spacing.small))

                    androidx.compose.material3.OutlinedTextField(
                        value = orderNote,
                        onValueChange = { orderNote = it },
                        placeholder = { Text("Add any special instructions or notes for this order") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Total Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total:",
                            style = MaterialTheme.typography.titleLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$${ShopUtils.getPriceShow(product.price * quantity.toUInt())}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        TechButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            style = TechButtonStyle.OUTLINE,
                            size = TechButtonSize.MEDIUM,
                            modifier = Modifier.weight(1f)
                        )
                        TechButton(
                            text = "Purchase",
                            onClick = { onConfirmBuy(selectedAddress, orderNote) },
                            style = TechButtonStyle.PRIMARY,
                            size = TechButtonSize.MEDIUM,
                            modifier = Modifier.weight(1f),
                            enabled = selectedAddress != null
                        )
                    }
                    
                    // Add bottom padding for safe area
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
            }
        }
    }
}