package com.focx.data.datasource.mock

import com.focx.domain.entity.NotificationSettings
import com.focx.domain.entity.StakingInfo
import com.focx.domain.entity.User
import com.focx.domain.entity.UserAddress
import com.focx.domain.entity.UserPreferences
import com.focx.domain.entity.UserProfile
import com.focx.domain.entity.WalletBalance
import com.focx.domain.repository.IUserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockUserDataSource @Inject constructor() : IUserRepository {

    private var currentUser: User? = null
    private var isLoggedIn = false

    override suspend fun getCurrentUser(): Flow<User?> = flow {
        delay(50)
        emit(currentUser)
    }

    override suspend fun getUserProfile(): Flow<UserProfile?> = flow {
        delay(50)
        emit(if (isLoggedIn) getSampleUserProfile() else null)
    }

    override suspend fun updateUserProfile(user: User): Result<User> {
        delay(100)
        currentUser = user
        return Result.success(user)
    }

    override suspend fun getUserAddresses(): Flow<List<UserAddress>> = flow {
        delay(50)
        emit(if (isLoggedIn) getSampleAddresses() else emptyList())
    }

    override suspend fun addUserAddress(address: UserAddress): Result<UserAddress> {
        delay(100)
        return Result.success(address)
    }

    override suspend fun updateUserAddress(address: UserAddress): Result<UserAddress> {
        delay(100)
        return Result.success(address)
    }

    override suspend fun deleteUserAddress(addressId: String): Result<Unit> {
        delay(100)
        return Result.success(Unit)
    }

    override suspend fun setDefaultAddress(addressId: String): Result<Unit> {
        delay(100)
        return Result.success(Unit)
    }

    override suspend fun getUserPreferences(): Flow<UserPreferences> = flow {
        delay(50)
        emit(getSamplePreferences())
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<UserPreferences> {
        delay(100)
        return Result.success(preferences)
    }

    override suspend fun loginWithWallet(walletAddress: String): Result<User> {
        val user = getSampleUser().copy(walletAddress = walletAddress)
        isLoggedIn = true
        currentUser = user
        return Result.success(user)
    }

    override suspend fun logout(): Result<Unit> {
        delay(50)
        isLoggedIn = false
        currentUser = null
        return Result.success(Unit)
    }


    fun isUserLoggedIn(): Boolean = isLoggedIn

    private fun getSampleUser(): User {
        return User(
            id = "user_001",
            walletAddress = null, // Will be set by real Solana wallet integration
            username = "Jim_chen",
            email = "jim.chen@example.com",
            avatarUrl = "https://secure.gravatar.com/avatar/ff8865c75b83ec102e9c99a2636f8c67?s=220&r=X&d=mm",
            isVerified = true,
            reputation = 4.8,
            totalSales = 25,
            totalPurchases = 42,
            memberSince = System.currentTimeMillis() - 86400000L * 30, // 30 days ago
            lastActive = System.currentTimeMillis()
        )
    }

    private fun getSampleUserProfile(): UserProfile {
        return UserProfile(
            user = getSampleUser(),
            walletBalance = WalletBalance(
                0.0,
                0.0,
                0.0,
                0.0
            ), // Default empty balance, will be updated by ProfileViewModel
            stakingInfo = getSampleStakingInfo(),
            addresses = getSampleAddresses(),
            preferences = getSamplePreferences()
        )
    }

    // Removed getSampleWalletBalance - now handled by real Solana wallet integration

    private fun getSampleStakingInfo(): StakingInfo {
        return StakingInfo(
            totalStaked = 500.0,
            availableToStake = 750.75,
            stakingRewards = 25.5,
            stakingApr = 8.5,
            unstakingPeriod = 7,
            lastStakeDate = System.currentTimeMillis() - 86400000L * 5, // 5 days ago
            nextRewardDate = System.currentTimeMillis() + 86400000L * 2 // 2 days from now
        )
    }

    private fun getSampleAddresses(): List<UserAddress> {
        return listOf(
            UserAddress(
                id = "addr_001",
                label = "Home",
                recipientName = "Jim Alice",
                addressLine1 = "123 Blockchain Street",
                addressLine2 = "Apt 4B",
                city = "San Francisco",
                state = "CA",
                postalCode = "94102",
                country = "United States",
                phoneNumber = "+1-555-0123",
                isDefault = true
            ),
            UserAddress(
                id = "addr_002",
                label = "Office",
                recipientName = "Jim Alice",
                addressLine1 = "456 Tech Avenue",
                addressLine2 = "Suite 200",
                city = "Palo Alto",
                state = "CA",
                postalCode = "94301",
                country = "United States",
                phoneNumber = "+1-555-0123",
                isDefault = false
            )
        )
    }

    private fun getSamplePreferences(): UserPreferences {
        return UserPreferences(
            language = "en",
            currency = "USDC",
            theme = "dark",
            notifications = NotificationSettings(
                orderUpdates = true,
                priceAlerts = true,
                governanceUpdates = true,
                stakingRewards = true,
                securityAlerts = true
            )
        )
    }
}