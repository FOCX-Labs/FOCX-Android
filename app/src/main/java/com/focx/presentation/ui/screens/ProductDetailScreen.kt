package com.focx.presentation.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.focx.presentation.ui.theme.Error
import com.focx.presentation.ui.theme.OnSurface
import com.focx.presentation.ui.theme.OnSurfaceVariant
import com.focx.presentation.ui.theme.Primary
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.ui.theme.Success
import com.focx.presentation.ui.theme.SurfaceDark
import com.focx.presentation.viewmodel.ProductListViewModel
import com.focx.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onBuyProduct: (Product, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    var isFavorite by remember { mutableStateOf(false) }
    var selectedQuantity by remember { mutableStateOf(1) }
    var showBuyDialog by remember { mutableStateOf(false) }

    val product = state.products.find { it.id == productId } ?: return

    val productImages = product.imageUrls

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
                            contentScale = ContentScale.Crop
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
                        text = product.name,
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
                            text = "$${product.price}",
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
                                .background(if (product.stock > 0) Success else Error)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = if (product.stock > 0) "In Stock (${product.stock} left)" else "Out of Stock",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (product.stock > 0) Success else Error
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Seller Info
                    Row(
                        modifier = Modifier.padding(Spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sold by ${product.sellerName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.large))

                    // Specifications
                    Text(
                        text = "Specifications",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    val specifications = product.specifications.toList()

                    specifications.forEach { (key, value) ->
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
                        if (specifications.last() != key to value) {
                            HorizontalDivider(
                                color = OnSurfaceVariant.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                        }
                    }

                    // Shipping Info
                    Text(
                        text = "Shipping Information",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    val shippingInfo = listOf(
                        "Origin" to product.shippingFrom,
                        "Shipping Range" to product.shippingTo.joinToString(),
                        "Logistics" to product.shippingMethods.joinToString()
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
                        text = product.description,
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
                        onClick = { if (selectedQuantity < product.stock) selectedQuantity++ },
                        style = TechButtonStyle.OUTLINE,
                        enabled = selectedQuantity < product.stock
                    )
                }

                // Buy Now Button
                TechButton(
                    text = "Buy Now",
                    onClick = { showBuyDialog = true },
                    style = TechButtonStyle.PRIMARY,
                    enabled = product.stock > 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Buy Dialog
    if (showBuyDialog) {
        BottomBuyDialog(
            product = product,
            quantity = selectedQuantity,
            addresses = profileState.userAddresses,
            onQuantityChange = { newQuantity ->
                selectedQuantity = newQuantity
            },
            onConfirmBuy = {
                onBuyProduct(product, selectedQuantity)
                showBuyDialog = false
            },
            onDismiss = {
                showBuyDialog = false
            }
        )
    }
}

@Composable
fun BottomBuyDialog(
    product: Product,
    quantity: Int,
    addresses: List<UserAddress>,
    onQuantityChange: (Int) -> Unit,
    onConfirmBuy: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAddress by remember { mutableStateOf(addresses.find { it.isDefault } ?: addresses.firstOrNull()) }


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
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.large)
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
                        text = "Confirm Purchase",
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
                            contentScale = ContentScale.Crop
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
                                text = "$${product.price}",
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
                                    Text(
                                        text = "${address.addressLine1}, ${address.city}, ${address.state} ${address.postalCode}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

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
                            text = "$${String.format("%.2f", product.price * quantity)}",
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
                            modifier = Modifier.weight(1f)
                        )
                        TechButton(
                            text = "Confirm Purchase",
                            onClick = onConfirmBuy,
                            style = TechButtonStyle.PRIMARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}