package com.focx.presentation.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.focx.presentation.ui.theme.Spacing
import com.focx.domain.entity.OrderManagementStatus
import com.focx.presentation.viewmodel.OrderStatusStep
import com.focx.presentation.viewmodel.SoldOrderDetailViewModel
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Paid
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoldOrderDetailScreen(
    orderId: String,
    activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender,
    onBackClick: () -> Unit,
    viewModel: SoldOrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    val context = LocalContext.current

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    // Show success message when tracking number is updated
    LaunchedEffect(state.trackingNumberJustUpdated) {
        if (state.trackingNumberJustUpdated) {
            Toast.makeText(context, "Tracking number updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Show error message when tracking update fails
    LaunchedEffect(state.trackingUpdateError) {
        state.trackingUpdateError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearTrackingUpdateError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Order $orderId",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Order ID", orderId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Order ID copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            state.error != null -> {
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
                            text = state.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        // TODO: Add retry button
                    }
                }
            }
            
            order != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.small))
                    }

                    // Order Status Card
                    item {
                        OrderStatusCard(
                            status = order.status,
                            trackingNumber = order.trackingNumber,
                            estimatedDelivery = order.estimatedDelivery?.let { 
                                java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                                    .format(java.util.Date(it * 1000))
                            },
                            onAddTrackingNumber = { trackingNumber ->
                                viewModel.updateTrackingNumber(order.id, trackingNumber, activityResultSender)
                            },
                            isUpdatingTracking = state.isUpdatingTracking
                        )
                    }

                    // Product Information
                    item {
                        if (order.items.isNotEmpty()) {
                            val firstItem = order.items.first()
                            ProductOrderCard(
                                productName = firstItem.productName,
                                price = firstItem.unitPrice,
                                quantity = firstItem.quantity,
                                totalAmount = firstItem.totalPrice,
                                productImage = firstItem.productImage
                            )
                        }
                    }

                    // Customer Information
                    item {
                        val shippingAddress = order.shippingAddress
                        if (shippingAddress != null) {
                            CustomerInfoCard(
                                customer = order.buyerId,
                                shippingAddress = shippingAddress.addressLine1,
                                note = order.orderNote?: "-",
                                paymentMethod = order.paymentMethod
                            )
                        }
                    }

                    // Order Timeline
                    item {
                        order?.let { orderData ->
                            OrderTimelineCard(orderSteps = viewModel.getOrderStatusSteps(orderData))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(Spacing.medium))
                    }
                }
            }
            
            else -> {
                // This shouldn't happen, but handle it just in case
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No order data available",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusCard(
    status: OrderManagementStatus,
    trackingNumber: String?,
    estimatedDelivery: String?,
    modifier: Modifier = Modifier,
    onAddTrackingNumber: ((String) -> Unit)? = null,
    isUpdatingTracking: Boolean = false
) {
    var showTrackingDialog by remember { mutableStateOf(false) }
    var inputTrackingNumber by remember { mutableStateOf("") }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Shipping",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(Spacing.small))
                
                Text(
                    text = when (status) {
                        OrderManagementStatus.Pending -> "Pending"
                        OrderManagementStatus.Shipped -> "Shipped"
                        OrderManagementStatus.Delivered -> "Delivered"
                        OrderManagementStatus.Refunded -> "Refunded"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            if (trackingNumber != null) {
                Text(
                    text = "Tracking number: $trackingNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(Spacing.small))
            }
            
            if (estimatedDelivery != null) {
                Text(
                    text = "Estimated Delivery: $estimatedDelivery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Add tracking button for pending orders
            if (status == OrderManagementStatus.Pending && onAddTrackingNumber != null) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                Button(
                    onClick = { showTrackingDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdatingTracking
                ) {
                    if (isUpdatingTracking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text("Updating...")
                    } else {
                        Text("Add Tracking Number")
                    }
                }
            }
        }
    }
    
    // Tracking number input dialog
    if (showTrackingDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isUpdatingTracking) {
                    showTrackingDialog = false
                    inputTrackingNumber = ""
                }
            },
            title = {
                Text("Add Tracking Number")
            },
            text = {
                Column {
                    Text(
                        text = "Input Tracking Number",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Spacing.medium)
                    )
                    OutlinedTextField(
                        value = inputTrackingNumber,
                        onValueChange = { inputTrackingNumber = it },
                        label = { Text("Tracking Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isUpdatingTracking
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputTrackingNumber.isNotBlank()) {
                            onAddTrackingNumber?.invoke(inputTrackingNumber)
                            showTrackingDialog = false
                            inputTrackingNumber = ""
                        }
                    },
                    enabled = inputTrackingNumber.isNotBlank() && !isUpdatingTracking
                ) {
                    if (isUpdatingTracking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text("Updating...")
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTrackingDialog = false
                        inputTrackingNumber = ""
                    },
                    enabled = !isUpdatingTracking
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProductOrderCard(
    productName: String,
    price: Double,
    quantity: Int,
    totalAmount: Double,
    productImage: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Text(
                text = "Product Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (productImage != null && productImage.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(productImage)
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
                                    text = productName.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$${price} × $quantity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$${totalAmount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CustomerInfoCard(
    customer: String,
    shippingAddress: String,
    note: String,
    paymentMethod: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Text(
                text = "Customer Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            CustomerInfoRow(
                icon = Icons.Default.Person,
                label = "Buyer",
                value = customer,
                onLongPress = { copyToClipboard(customer, "Buyer ID") }
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            CustomerInfoRow(
                icon = Icons.Default.Home,
                label = "Address",
                value = shippingAddress,
                onLongPress = { copyToClipboard(shippingAddress, "Shipping Address") }
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            CustomerInfoRow(
                icon = Icons.AutoMirrored.Filled.Note,
                label = "Note",
                value = note,
                onLongPress = { copyToClipboard(note, "Order Note") }
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Paid,
                    contentDescription = "Payment",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Column {
                    Text(
                        text = "Payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = paymentMethod,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    copyToClipboard(paymentMethod, "Payment Method")
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(Spacing.small))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = if (onLongPress != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongPress() }
                        )
                    }
                } else {
                    Modifier
                }
            )
        }
    }
}

@Composable
fun OrderTimelineCard(
    orderSteps: List<OrderStatusStep>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Text(
                text = "Order Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            orderSteps.forEachIndexed { index, step ->
                TimelineStep(
                    step = step,
                    isLast = index == orderSteps.lastIndex
                )
                if (index < orderSteps.lastIndex) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
            }
        }
    }
}

@Composable
fun TimelineStep(
    step: OrderStatusStep,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            step.isCompleted -> Color(0xFF4CAF50)
                            step.isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                ) {}
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(1.dp))
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (step.isCompleted) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    ) {}
                }
            }
        }

        Spacer(modifier = Modifier.width(Spacing.medium))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (step.isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = if (step.isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = step.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}