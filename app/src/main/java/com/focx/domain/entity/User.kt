package com.focx.domain.entity

data class User(
    val id: String,
    val walletAddress: String? = null,
    val username: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val reputation: Double = 0.0,
    val totalSales: Int = 0,
    val totalPurchases: Int = 0,
    val memberSince: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis()
)

data class UserProfile(
    val user: User,
    val walletBalance: WalletBalance,
    val stakingInfo: StakingInfo,
    val addresses: List<UserAddress>,
    val preferences: UserPreferences
)

data class UserAddress(
    val id: String,
    val label: String,
    val recipientName: String,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val phoneNumber: String,
    val isDefault: Boolean = false
)

data class UserPreferences(
    val currency: String = "USDC",
    val language: String = "en",
    val theme: String = "dark",
    val notifications: NotificationSettings
)

data class NotificationSettings(
    val orderUpdates: Boolean = true,
    val priceAlerts: Boolean = true,
    val governanceUpdates: Boolean = true,
    val stakingRewards: Boolean = true,
    val securityAlerts: Boolean = true
)

fun UserAddress.toShippingAddress(): ShippingAddress = ShippingAddress(
    recipientName = this.recipientName,
    addressLine1 = this.addressLine1,
    addressLine2 = this.addressLine2,
    city = this.city,
    state = this.state,
    postalCode = this.postalCode,
    country = this.country,
    phoneNumber = this.phoneNumber
)