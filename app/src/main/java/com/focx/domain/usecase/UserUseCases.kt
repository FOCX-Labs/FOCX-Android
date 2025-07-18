package com.focx.domain.usecase

import com.focx.domain.entity.User
import com.focx.domain.entity.UserAddress
import com.focx.domain.entity.UserPreferences
import com.focx.domain.entity.UserProfile
import com.focx.domain.repository.IUserRepository
import com.focx.utils.Log
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Flow<User?> {
        return userRepository.getCurrentUser()
    }
}

class GetUserProfileUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Flow<UserProfile?> {
        return userRepository.getUserProfile()
    }
}

class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(user: User): Result<User> {
        return userRepository.updateUserProfile(user)
    }
}

class GetUserAddressesUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Flow<List<UserAddress>> {
        return userRepository.getUserAddresses()
    }
}

class AddUserAddressUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(address: UserAddress): Result<UserAddress> {
        return userRepository.addUserAddress(address)
    }
}

class UpdateUserAddressUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(address: UserAddress): Result<UserAddress> {
        return userRepository.updateUserAddress(address)
    }
}

class DeleteUserAddressUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(addressId: String): Result<Unit> {
        return userRepository.deleteUserAddress(addressId)
    }
}

class SetDefaultAddressUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(addressId: String): Result<Unit> {
        return userRepository.setDefaultAddress(addressId)
    }
}

class GetUserPreferencesUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Flow<UserPreferences> {
        return userRepository.getUserPreferences()
    }
}

class UpdateUserPreferencesUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(preferences: UserPreferences): Result<UserPreferences> {
        return userRepository.updateUserPreferences(preferences)
    }
}

class LoginWithWalletUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(walletAddress: String): Result<User> {
        val result = userRepository.loginWithWallet(walletAddress)
        if (result.isFailure) {
            Log.e(
                "LoginWithWalletUseCase",
                "Login failed for wallet: $walletAddress, error: ${result.exceptionOrNull()?.message}"
            )
        }
        return result
    }
}