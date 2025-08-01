package com.focx.data.datasource.local

import android.content.Context
import android.content.SharedPreferences
import com.focx.data.constants.PreferencesConstants
import com.focx.domain.entity.UserAddress
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    
    companion object {
        private const val ADDRESSES_KEY = "user_addresses"
        private const val DEFAULT_ADDRESS_ID_KEY = "default_address_id"
        private const val INITIALIZED_KEY = "addresses_initialized"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PreferencesConstants.USER_PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get all user addresses from local storage
     */
    fun getUserAddresses(): Flow<List<UserAddress>> = flow {
        val addressesJson = prefs.getString(ADDRESSES_KEY, "[]")
        val addresses = try {
            val type = object : TypeToken<List<UserAddress>>() {}.type
            gson.fromJson<List<UserAddress>>(addressesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        
        // Initialize sample addresses if no addresses exist and not initialized before
        if (addresses.isEmpty() && !prefs.getBoolean(INITIALIZED_KEY, false)) {
            initializeSampleAddresses()
            val sampleAddresses = getSampleAddresses()
            emit(sampleAddresses)
        } else {
            emit(addresses)
        }
    }
    
    /**
     * Initialize sample addresses for first-time users
     */
    private fun initializeSampleAddresses() {
        val sampleAddresses = getSampleAddresses()
        val addressesJson = gson.toJson(sampleAddresses)
        prefs.edit()
            .putString(ADDRESSES_KEY, addressesJson)
            .putString(DEFAULT_ADDRESS_ID_KEY, sampleAddresses.first().id)
            .putBoolean(INITIALIZED_KEY, true)
            .apply()
    }
    
    /**
     * Get sample addresses for demonstration
     */
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
    
    /**
     * Save address to local storage
     */
    suspend fun saveAddress(address: UserAddress): Result<UserAddress> {
        return try {
            val currentAddresses = getCurrentAddresses().toMutableList()
            
            // If this is a new address, add it to the list
            val existingIndex = currentAddresses.indexOfFirst { it.id == address.id }
            if (existingIndex != -1) {
                // Update existing address
                currentAddresses[existingIndex] = address
            } else {
                // Add new address
                currentAddresses.add(address)
            }
            
            // If this address is set as default, update other addresses
            if (address.isDefault) {
                currentAddresses.forEachIndexed { index, addr ->
                    if (addr.id != address.id) {
                        currentAddresses[index] = addr.copy(isDefault = false)
                    }
                }
                prefs.edit().putString(DEFAULT_ADDRESS_ID_KEY, address.id).apply()
            }
            
            // Save updated addresses
            val addressesJson = gson.toJson(currentAddresses)
            prefs.edit().putString(ADDRESSES_KEY, addressesJson).apply()
            
            Result.success(address)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete address from local storage
     */
    suspend fun deleteAddress(addressId: String): Result<Unit> {
        return try {
            val currentAddresses = getCurrentAddresses().toMutableList()
            val addressToDelete = currentAddresses.find { it.id == addressId }
            
            if (addressToDelete != null) {
                currentAddresses.removeAll { it.id == addressId }
                
                // If deleted address was default, set first remaining address as default
                if (addressToDelete.isDefault && currentAddresses.isNotEmpty()) {
                    currentAddresses[0] = currentAddresses[0].copy(isDefault = true)
                    prefs.edit().putString(DEFAULT_ADDRESS_ID_KEY, currentAddresses[0].id).apply()
                } else if (currentAddresses.isEmpty()) {
                    prefs.edit().remove(DEFAULT_ADDRESS_ID_KEY).apply()
                }
                
                val addressesJson = gson.toJson(currentAddresses)
                prefs.edit().putString(ADDRESSES_KEY, addressesJson).apply()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Set address as default
     */
    suspend fun setDefaultAddress(addressId: String): Result<Unit> {
        return try {
            val currentAddresses = getCurrentAddresses().toMutableList()
            
            currentAddresses.forEachIndexed { index, address ->
                currentAddresses[index] = address.copy(isDefault = address.id == addressId)
            }
            
            prefs.edit().putString(DEFAULT_ADDRESS_ID_KEY, addressId).apply()
            
            val addressesJson = gson.toJson(currentAddresses)
            prefs.edit().putString(ADDRESSES_KEY, addressesJson).apply()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current addresses from SharedPreferences (synchronous)
     */
    private fun getCurrentAddresses(): List<UserAddress> {
        val addressesJson = prefs.getString(ADDRESSES_KEY, "[]")
        return try {
            val type = object : TypeToken<List<UserAddress>>() {}.type
            gson.fromJson<List<UserAddress>>(addressesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear all addresses (for logout)
     */
    fun clearAllAddresses() {
        prefs.edit()
            .remove(ADDRESSES_KEY)
            .remove(DEFAULT_ADDRESS_ID_KEY)
            .apply()
    }
} 