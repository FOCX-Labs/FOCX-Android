package com.focx.data.datasource.local

import android.content.Context
import android.content.SharedPreferences
import com.focx.domain.entity.Product
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendProductCacheDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "recommend_product_cache",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_RECOMMEND_PRODUCTS = "recommend_products"
        private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
        private const val CACHE_DURATION = 3 * 24 * 60 * 1000L // 3 days cache duration
    }

    fun getCachedRecommendProducts(): List<Product>? {
        val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - timestamp > CACHE_DURATION) {
            clearCache()
            return null
        }
        
        val productsJson = prefs.getString(KEY_RECOMMEND_PRODUCTS, null)
        return if (productsJson != null) {
            try {
                val type = object : TypeToken<List<Product>>() {}.type
                Gson().fromJson<List<Product>>(productsJson, type) ?: emptyList()
            } catch (e: Exception) {
                clearCache()
                null
            }
        } else {
            null
        }
    }

    fun cacheRecommendProducts(products: List<Product>) {
        try {
            val productsJson = Gson().toJson(products)
            prefs.edit()
                .putString(KEY_RECOMMEND_PRODUCTS, productsJson)
                .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            clearCache()
        }
    }

    fun hasValidCache(): Boolean {
        val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()
        return (currentTime - timestamp) <= CACHE_DURATION && 
               prefs.contains(KEY_RECOMMEND_PRODUCTS)
    }

    fun clearCache() {
        prefs.edit()
            .remove(KEY_RECOMMEND_PRODUCTS)
            .remove(KEY_CACHE_TIMESTAMP)
            .apply()
    }

    fun getCacheTimestamp(): Long {
        return prefs.getLong(KEY_CACHE_TIMESTAMP, 0L)
    }
}
