package com.focx.data.datasource.mock

import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderItem
import com.focx.domain.entity.OrderManagementStatus
import com.focx.domain.entity.ShippingAddress
import com.focx.domain.repository.IOrderRepository
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockOrderDataSource @Inject constructor() : IOrderRepository {

    private val mockOrders = mutableListOf(
        Order(
            id = "order_001",
            buyerId = "buyer_001",
            sellerId = "seller1",
            sellerName = "TechStore Official",
            items = listOf(
                OrderItem(
                    id = "item_001",
                    productId = "1",
                    productName = "iPhone 15 Pro Max  Apple",
                    productImage = "https://example.com/iphone1.jpg",
                    quantity = 1,
                    unitPrice = 1199.99,
                    totalPrice = 1199.99
                )
            ),
            totalAmount = 1199.99,
            currency = "USDC",
            status = OrderManagementStatus.Delivered,
            shippingAddress = ShippingAddress(
                recipientName = "John Smith",
                addressLine1 = "123 Main St",
                addressLine2 = "Apt 4B",
                city = "New York",
                state = "NY",
                postalCode = "10001",
                country = "USA",
                phoneNumber = "+1 212-555-1234"
            ),
            paymentMethod = "USDC",
            transactionHash = "0x1234567890abcdef",
            trackingNumber = "SF1234567890",
            orderDate = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000, // 7 days ago
            updatedAt = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000, // 1 day ago
            estimatedDelivery = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000 // in 2 days
        ),
        Order(
            id = "order_002",
            buyerId = "buyer_001",
            sellerId = "seller2",
            sellerName = "Sneaker Paradise",
            items = listOf(
                OrderItem(
                    id = "item_002",
                    productId = "3",
                    productName = "Nike Air Jordan 1 Retro High",
                    productImage = "https://example.com/jordan1.jpg",
                    quantity = 1,
                    unitPrice = 170.00,
                    totalPrice = 170.00
                )
            ),
            totalAmount = 170.00,
            currency = "USDC",
            status = OrderManagementStatus.Shipped,
            shippingAddress = ShippingAddress(
                recipientName = "John Smith",
                addressLine1 = "123 Main St",
                addressLine2 = "Apt 4B",
                city = "New York",
                state = "NY",
                postalCode = "10001",
                country = "USA",
                phoneNumber = "+1 212-555-1234"
            ),
            paymentMethod = "USDC",
            transactionHash = "0xabcdef1234567890",
            trackingNumber = "SF0987654321",
            orderDate = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000, // 3 days ago
            updatedAt = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000, // 1 day ago
            estimatedDelivery = System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000 // in 1 day
        ),
        Order(
            id = "order_003",
            buyerId = "buyer_002",
            sellerId = "seller1",
            sellerName = "TechStore Official",
            items = listOf(
                OrderItem(
                    id = "item_003",
                    productId = "2",
                    productName = "MacBook Pro 16\" M3 Max",
                    productImage = "https://example.com/macbook1.jpg",
                    quantity = 1,
                    unitPrice = 3499.99,
                    totalPrice = 3499.99
                )
            ),
            totalAmount = 3499.99,
            currency = "USDC",
            status = OrderManagementStatus.Pending,
            shippingAddress = ShippingAddress(
                recipientName = "Jane Doe",
                addressLine1 = "456 Oak Ave",
                addressLine2 = "",
                city = "London",
                state = "",
                postalCode = "SW1A 0AA",
                country = "United Kingdom",
                phoneNumber = "+44 20 7946 0958"
            ),
            paymentMethod = "USDC",
            transactionHash = "0xfedcba0987654321",
            trackingNumber = null,
            orderDate = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000, // 1 day ago
            updatedAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000, // 2 hours ago
            estimatedDelivery = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000 // in 3 days
        ),
        Order(
            id = "order_004",
            buyerId = "buyer_001",
            sellerId = "seller3",
            sellerName = "EV Accessories",
            items = listOf(
                OrderItem(
                    id = "item_004",
                    productId = "4",
                    productName = "Tesla Model Y Car Charger",
                    productImage = "https://example.com/tesla1.jpg",
                    quantity = 2,
                    unitPrice = 299.99,
                    totalPrice = 599.98
                )
            ),
            totalAmount = 599.98,
            currency = "USDC",
            status = OrderManagementStatus.Pending,
            shippingAddress = ShippingAddress(
                recipientName = "John Smith",
                addressLine1 = "123 Main St",
                addressLine2 = "Apt 4B",
                city = "New York",
                state = "NY",
                postalCode = "10001",
                country = "USA",
                phoneNumber = "+1 212-555-1234"
            ),
            paymentMethod = "USDC",
            transactionHash = null,
            trackingNumber = null,
            orderDate = System.currentTimeMillis() - 2 * 60 * 60 * 1000, // 2 hours ago
            updatedAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000, // 2 hours ago
            estimatedDelivery = null
        )
    )

    override suspend fun getOrders(): Flow<List<Order>> = flow {
        delay(400)
        emit(mockOrders.toList())
    }

    override suspend fun getOrderById(id: String): Order? {
        delay(200)
        return mockOrders.find { it.id == id }
    }

    override suspend fun getOrdersByBuyer(buyerId: String): Flow<List<Order>> = flow {
        delay(300)
        emit(mockOrders.filter { it.buyerId == buyerId })
    }
    
    override suspend fun getOrdersByBuyerPaged(buyerId: String, page: Int, pageSize: Int): Flow<List<Order>> = flow {
        delay(300)
        val allBuyerOrders = mockOrders.filter { it.buyerId == buyerId }
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, allBuyerOrders.size)
        
        if (startIndex < allBuyerOrders.size) {
            emit(allBuyerOrders.subList(startIndex, endIndex))
        } else {
            emit(emptyList())
        }
    }

    override suspend fun getOrdersBySeller(sellerId: String): Flow<List<Order>> = flow {
        delay(300)
        emit(mockOrders.filter { it.sellerId == sellerId })
    }
    
    override suspend fun getOrdersBySellerPaged(sellerId: String, page: Int, pageSize: Int): Flow<List<Order>> = flow {
        delay(300)
        val allSellerOrders = mockOrders.filter { it.sellerId == sellerId }
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, allSellerOrders.size)
        
        if (startIndex < allSellerOrders.size) {
            emit(allSellerOrders.subList(startIndex, endIndex))
        } else {
            emit(emptyList())
        }
    }

    override suspend fun getOrdersByStatus(status: OrderManagementStatus): Flow<List<Order>> = flow {
        delay(300)
        emit(mockOrders.filter { it.status == status })
    }

    override suspend fun createOrder(product: com.focx.domain.entity.Product, quantity: UInt, buyer: String, shippingAddress: com.focx.domain.entity.ShippingAddress, orderNote: String, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender): Result<Order> {
        delay(500)
        
        // Create order from product
        val newOrder = Order(
            id = "order_${System.currentTimeMillis()}",
            buyerId = buyer,
            sellerId = product.sellerId,
            sellerName = product.sellerName,
            items = listOf(
                OrderItem(
                    id = "item_${System.currentTimeMillis()}",
                    productId = product.id.toString(),
                    productName = product.name,
                    productImage = product.imageUrls.firstOrNull() ?: "",
                    quantity = quantity.toInt(),
                    unitPrice = (product.price.toDouble() / 1000000), // Convert from micro units
                    totalPrice = (product.price.toDouble() / 1000000) * quantity.toDouble()
                )
            ),
            totalAmount = (product.price.toDouble() / 1000000) * quantity.toDouble(),
            currency = "USDC",
            status = OrderManagementStatus.Pending,
            shippingAddress = shippingAddress,
            orderNote = orderNote,
            paymentMethod = "USDC",
            transactionHash = null,
            trackingNumber = null,
            orderDate = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            estimatedDelivery = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000 // 7 days from now
        )
        
        mockOrders.add(newOrder)
        return Result.success(newOrder)
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderManagementStatus): Result<Unit> {
        delay(400)
        val orderIndex = mockOrders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            mockOrders[orderIndex] = mockOrders[orderIndex].copy(
                status = status,
                updatedAt = System.currentTimeMillis()
            )
            return Result.success(Unit)
        }
        return Result.failure(Exception("Order not found"))
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> {
        return updateOrderStatus(orderId, OrderManagementStatus.Refunded)
    }

    override suspend fun updateTrackingNumber(orderId: String, trackingNumber: String, merchantPubKey: SolanaPublicKey, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender): Result<Unit> {
        delay(300)
        val orderIndex = mockOrders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            mockOrders[orderIndex] = mockOrders[orderIndex].copy(
                trackingNumber = trackingNumber,
                updatedAt = System.currentTimeMillis()
            )
            return Result.success(Unit)
        }
        return Result.failure(Exception("Order not found"))
    }

    override suspend fun confirmReceipt(orderId: String, buyerPubKey: SolanaPublicKey, merchantPubKey: SolanaPublicKey, activityResultSender: com.solana.mobilewalletadapter.clientlib.ActivityResultSender): Result<Unit> {
        delay(400)
        val orderIndex = mockOrders.indexOfFirst { it.id == orderId }
        if (orderIndex != -1) {
            mockOrders[orderIndex] = mockOrders[orderIndex].copy(
                status = OrderManagementStatus.Delivered,
                updatedAt = System.currentTimeMillis()
            )
            return Result.success(Unit)
        }
        return Result.failure(Exception("Order not found"))
    }
}