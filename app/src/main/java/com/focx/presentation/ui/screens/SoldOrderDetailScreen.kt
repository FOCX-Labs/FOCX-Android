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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focx.presentation.ui.theme.Spacing

data class SoldOrderDetail(
    val orderId: String,
    val productName: String,
    val productPrice: Double,
    val quantity: Int,
    val totalAmount: Double,
    val orderDate: String,
    val status: String,
    val customerName: String,
    val customerPhone: String,
    val shippingAddress: String,
    val paymentMethod: String,
    val trackingNumber: String?,
    val estimatedDelivery: String?
)

data class OrderStatusStep(
    val title: String,
    val description: String,
    val timestamp: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoldOrderDetailScreen(
    orderId: String,
    onBackClick: () -> Unit
) {
    val orderDetail = remember {
        SoldOrderDetail(
            orderId = orderId,
            productName = "iPhone 15 Pro Max  Apple",
            productPrice = 1199.99,
            quantity = 1,
            totalAmount = 1199.99,
            orderDate = "2024-01-15 14:30",
            status = "Shipped",
            customerName = "John Smith",
            customerPhone = "+1 (555) 123-4567",
            shippingAddress = "123 Main St, Apt 4B\nNew York, NY 10001\nUnited States",
            paymentMethod = "Credit Card (**** 1234)",
            trackingNumber = "1Z999AA1234567890",
            estimatedDelivery = "Jan 18, 2024"
        )
    }

    val orderSteps = remember {
        listOf(
            OrderStatusStep(
                title = "Order Placed",
                description = "Customer placed the order",
                timestamp = "Jan 15, 2:30 PM",
                isCompleted = true,
                isCurrent = false
            ),
            OrderStatusStep(
                title = "Payment Confirmed",
                description = "Payment successfully processed",
                timestamp = "Jan 15, 2:31 PM",
                isCompleted = true,
                isCurrent = false
            ),
            OrderStatusStep(
                title = "Processing",
                description = "Order is being prepared",
                timestamp = "Jan 15, 3:00 PM",
                isCompleted = true,
                isCurrent = false
            ),
            OrderStatusStep(
                title = "Shipped",
                description = "Package has been shipped",
                timestamp = "Jan 16, 10:00 AM",
                isCompleted = true,
                isCurrent = true
            ),
            OrderStatusStep(
                title = "Delivered",
                description = "Package delivered to customer",
                timestamp = "Estimated: Jan 18",
                isCompleted = false,
                isCurrent = false
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order #${orderDetail.orderId}") },
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
                    status = orderDetail.status,
                    trackingNumber = orderDetail.trackingNumber,
                    estimatedDelivery = orderDetail.estimatedDelivery
                )
            }

            // Product Information
            item {
                ProductOrderCard(
                    productName = orderDetail.productName,
                    price = orderDetail.productPrice,
                    quantity = orderDetail.quantity,
                    totalAmount = orderDetail.totalAmount
                )
            }

            // Customer Information
            item {
                CustomerInfoCard(
                    customerName = orderDetail.customerName,
                    customerPhone = orderDetail.customerPhone,
                    shippingAddress = orderDetail.shippingAddress,
                    paymentMethod = orderDetail.paymentMethod
                )
            }

            // Order Timeline
            item {
                OrderTimelineCard(orderSteps = orderSteps)
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.medium))
            }
        }
    }
}

@Composable
fun OrderStatusCard(
    status: String,
    trackingNumber: String?,
    estimatedDelivery: String?,
    modifier: Modifier = Modifier
) {
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
                    text = "Order Status: $status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (trackingNumber != null) {
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "Tracking: $trackingNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (estimatedDelivery != null) {
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "Estimated Delivery: $estimatedDelivery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ProductOrderCard(
    productName: String,
    price: Double,
    quantity: Int,
    totalAmount: Double,
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
                // Product Image Placeholder
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
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
                                text = productName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
    customerName: String,
    customerPhone: String,
    shippingAddress: String,
    paymentMethod: String,
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
                text = "Customer Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            CustomerInfoRow(
                icon = Icons.Default.Person,
                label = "Customer",
                value = customerName
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            CustomerInfoRow(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = customerPhone
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            CustomerInfoRow(
                icon = Icons.Default.Place,
                label = "Address",
                value = shippingAddress
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Person, // Using Person as placeholder for payment
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
                        style = MaterialTheme.typography.bodyMedium
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
    modifier: Modifier = Modifier
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
                style = MaterialTheme.typography.bodyMedium
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