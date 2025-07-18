package com.focx.domain.repository

import com.focx.domain.entity.User
import com.focx.domain.entity.UserAddress
import com.focx.domain.entity.UserPreferences
import com.focx.domain.entity.UserProfile
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    suspend fun getCurrentUser(): Flow<User?>
    suspend fun getUserProfile(): Flow<UserProfile?>
    suspend fun updateUserProfile(user: User): Result<User>
    suspend fun getUserAddresses(): Flow<List<UserAddress>>
    suspend fun addUserAddress(address: UserAddress): Result<UserAddress>
    suspend fun updateUserAddress(address: UserAddress): Result<UserAddress>
    suspend fun deleteUserAddress(addressId: String): Result<Unit>
    suspend fun setDefaultAddress(addressId: String): Result<Unit>
    suspend fun getUserPreferences(): Flow<UserPreferences>
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<UserPreferences>
    suspend fun loginWithWallet(walletAddress: String): Result<User>
    suspend fun logout(): Result<Unit>
}