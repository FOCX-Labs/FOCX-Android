package com.focx.data.datasource.solana


import android.content.Context
import android.text.TextUtils
import com.focx.core.constants.AppConstants
import com.focx.core.network.NetworkConfig
import com.focx.core.network.NetworkConnectionManager
import com.focx.data.datasource.mock.mockProducts
import com.focx.data.datasource.local.RecommendProductCacheDataSource
import com.focx.domain.entity.AddProductToKeywordIndex
import com.focx.domain.entity.AddProductToPriceIndex
import com.focx.domain.entity.AddProductToSalesIndex
import com.focx.domain.entity.CreateProductBase
import com.focx.domain.entity.CreateProductExtended
import com.focx.domain.entity.DeleteProduct
import com.focx.domain.entity.IdChunk
import com.focx.domain.entity.KeywordRoot
import com.focx.domain.entity.KeywordShard
import com.focx.domain.entity.MerchantIdAccount
import com.focx.domain.entity.PriceIndexNode
import com.focx.domain.entity.Product
import com.focx.domain.entity.SalesIndexNode
import com.focx.domain.entity.UpdateProduct
import com.focx.domain.repository.IProductRepository
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.focx.utils.Utils
import com.focx.utils.ShopUtils.genTransactionInstruction
import com.funkatronics.encoders.Base58
import com.funkatronics.kborsh.Borsh
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.programs.SystemProgram
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.getAccountInfo
import com.solana.serialization.AnchorInstructionSerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class SolanaProductDataSource @Inject constructor(
    private val context: Context,
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val networkConnectionManager: NetworkConnectionManager
) : IProductRepository {
    companion object {
        private const val TAG = "SPDS"
    }

    private var products: MutableList<Product> = mockProducts.toMutableList()
    
    private val recommendProductCacheDataSource by lazy {
        RecommendProductCacheDataSource(context)
    }

    override suspend fun getProducts(
        page: Int,
        pageSize: Int,
        refresh: Boolean
    ): Flow<List<Product>> = flow {
        if (!refresh && page == 1) {
            val cachedProducts = recommendProductCacheDataSource.getCachedRecommendProducts()
            if (cachedProducts != null) {
                val start = 0
                val end = minOf(pageSize, cachedProducts.size)
                emit(cachedProducts.subList(start, end))
                
                try {
                    val freshProducts = getProductsFromChain(page, pageSize)
                    if (freshProducts.isNotEmpty()) {
                        recommendProductCacheDataSource.cacheRecommendProducts(freshProducts)
                        if (freshProducts != cachedProducts) {
                            val freshStart = 0
                            val freshEnd = minOf(pageSize, freshProducts.size)
                            emit(freshProducts.subList(freshStart, freshEnd))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch fresh recommend products: ${e.message}", e)
                }
                return@flow
            }
        }
        
        val products = getProductsFromChain(page, pageSize)
        
        if (page == 1 && products.isNotEmpty()) {
            recommendProductCacheDataSource.cacheRecommendProducts(products)
        }
        
        emit(products)
    }

    private suspend fun getProductsFromChain(
        page: Int,
        pageSize: Int,
        keyword: String? = null,
        priceRange: Pair<ULong, ULong>? = null,
        salesRange: Pair<UInt, UInt>? = null
    ): List<Product> {
        val ids = ArrayList<ULong>()
        if (!TextUtils.isEmpty(keyword)) {
            ids.addAll(searchByKeywordFormChain(keyword!!))
        }
        if (priceRange != null) {
            ids.addAll(searchByPriceRangeFormChain(priceRange))
        }
        if (salesRange != null) {
            ids.addAll(searchBySalesRangeFormChain(salesRange))
        }

        if (keyword == null && priceRange == null && salesRange == null) {
            ids.addAll(getRecommendIds())
        }

        val distinctIds = ids.distinct()


        val start = (page - 1) * pageSize
        val end = minOf(start + pageSize, distinctIds.size)
        if (start >= distinctIds.size) {
            return emptyList()
        } else {
            return distinctIds.subList(start, end)
                .mapNotNull { id -> ShopUtils.getProductInfoById(id, networkConnectionManager.getSolanaRpcClient()) }
        }
    }

    private fun getRecommendIds(): List<ULong> {
        return listOf(
            670001UL,
            1010000UL,
            1010001UL,
            1010002UL,
            1010003UL,
            1010005UL,
            1010006UL,
            1010007UL,
            1010008UL,
            1010009UL,
            1010010UL,
            1010011UL,
            1010012UL,
            1010013UL,
            1010014UL
        )
    }




    private suspend fun searchByKeywordFormChain(keyword: String): List<ULong> {
        val keywordRootPda = ShopUtils.getKeywordRootPda(keyword).getOrNull()!!
        val keywordShardPda = ShopUtils.getTargetShardPda(keyword).getOrNull()!!

        val keywordRoot = networkConnectionManager.getSolanaRpcClient().getAccountInfo<KeywordRoot>(keywordRootPda).result?.data
        val result =
            networkConnectionManager.getSolanaRpcClient().getAccountInfo<KeywordShard>(keywordShardPda).result?.data?.productIds

        return result ?: emptyList()
    }

    private suspend fun searchByPriceRangeFormChain(priceRange: Pair<ULong, ULong>): List<ULong> {
        val priceIndexPda = ShopUtils.getPriceIndexPDA(priceRange).getOrNull()!!

        val result =
            networkConnectionManager.getSolanaRpcClient().getAccountInfo<PriceIndexNode>(priceIndexPda).result?.data?.productIds

        return result ?: emptyList()
    }

    private suspend fun searchBySalesRangeFormChain(salesRange: Pair<UInt, UInt>): List<ULong> {
        val salesIndexPda = ShopUtils.getSalesIndexPDA(salesRange).getOrNull()!!

        val result =
            networkConnectionManager.getSolanaRpcClient().getAccountInfo<SalesIndexNode>(salesIndexPda).result?.data?.productIds

        return result ?: emptyList()
    }

    override suspend fun getProductById(productId: ULong): Flow<Product?> = flow {
        val product = ShopUtils.getProductInfoById(productId, networkConnectionManager.getSolanaRpcClient())
        emit(product)
    }

    suspend fun getMerchantProducts(merchantAddress: String): Flow<Result<List<Product>>> = flow {
        try {
            Log.d(TAG, "Getting merchant products for: $merchantAddress")
            val merchantPublicKey = SolanaPublicKey.from(merchantAddress)

            val products = ShopUtils.getMerchantProducts(merchantPublicKey, networkConnectionManager.getSolanaRpcClient())
            emit(Result.success(products))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get merchant products: ${e.message}", e)
            emit(Result.failure(Exception("Failed to get merchant products: ${e.message}")))
        }
    }
    
    suspend fun getMerchantProductsPaged(merchantAddress: String, page: Int, pageSize: Int): Flow<Result<List<Product>>> = flow {
        try {
            Log.d(TAG, "Getting merchant products paged for: $merchantAddress, page: $page, pageSize: $pageSize")
            val merchantPublicKey = SolanaPublicKey.from(merchantAddress)

            // Use ShopUtils pagination directly instead of getting all products
            val products = ShopUtils.getMerchantProducts(
                merchantPublicKey, 
                networkConnectionManager.getSolanaRpcClient(),
                page = page,
                pageSize = pageSize
            )
            Log.d(TAG, "Received ${products.size} products for page $page")
            emit(Result.success(products))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get merchant products paged: ${e.message}", e)
            emit(Result.failure(Exception("Failed to get merchant products: ${e.message}")))
        }
    }

    override suspend fun searchProducts(
        query: String,
        page: Int,
        pageSize: Int
    ): Flow<List<Product>> =
        flow {
            emit(getProductsFromChain(page, pageSize, keyword = query))
        }

    override suspend fun getProductsByCategory(
        category: String,
        page: Int,
        pageSize: Int
    ): Flow<List<Product>> = flow {
        delay(500)
        val filtered = mockProducts.filter { it.category == category }
        val start = (page - 1) * pageSize
        val end = minOf(start + pageSize, filtered.size)
        if (start >= filtered.size) {
            emit(emptyList())
        } else {
            emit(filtered.subList(start, end))
        }
    }

    override suspend fun getRelatedProducts(productId: ULong, count: Int): Flow<List<Product>> =
        flow {
            delay(500)
            val product = mockProducts.find { it.id == productId }
            if (product != null) {
                val related = mockProducts.filter {
                    it.category == product.category && it.id != productId
                }.take(count)
                emit(related)
            } else {
                emit(emptyList())
            }
        }

    override suspend fun saveProduct(
        product: Product,
        accountPublicKey: String,
        activityResultSender: ActivityResultSender
    ) {
        try {
            Log.d(TAG, "Starting saveProduct for product: ${product.name}")
            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(
                    TAG, "saveProduct authResult.authToken:${authResult.authToken}"
                )

                val builder = Message.Builder()
                val instructions = genAddProductInstructions(product, accountPublicKey)

                instructions.forEach { ix -> builder.addInstruction(ix) }
                val recentBlockhash = try {
                    withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }
                val message = builder.setRecentBlockhash(recentBlockhash).build()

                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (saveProduct): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (saveProduct): $signResult")
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(
                            TAG,
                            "Product saved successfully: $signatureString"
                        )
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(networkConnectionManager.getSolanaRpcClient(), signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d(TAG, "Transaction confirmed: $signatureString")
                            // Add to local list only after successful blockchain transaction
                            products.add(product)
                        } else {
                            Log.e(TAG, "Transaction confirmation failed: $signatureString")
                            throw Exception("Transaction confirmation failed")
                        }
                    } else {
                        Log.e(TAG, "No signature returned from saveProduct transaction")
                        throw Exception("No signature returned from transaction")
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for saveProduct")
                    throw Exception("No wallet found")
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "saveProduct failed: ${result.e.message}")
                    throw Exception("Transaction failed: ${result.e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveProduct exception:", e)
            throw Exception("Failed to save product: ${e.message}")
        }
    }

    private suspend fun genAddProductInstructions(
        product: Product,
        accountPublicKey: String
    ): List<TransactionInstruction> {

        val accountPublicKey = SolanaPublicKey.from(accountPublicKey)
        val globalRootPDA = ShopUtils.getGlobalRootPda().getOrNull()!!
        val merchantIdAccountPDA = ShopUtils.getMerchantIdPda(accountPublicKey).getOrNull()!!
        val paymentConfigPDA = ShopUtils.getPaymentPda().getOrNull()!!
        val nextProductId = getNextProductId(accountPublicKey, merchantIdAccountPDA)
        val productBasePDA = ShopUtils.getProductBasePDA(nextProductId).getOrNull()!!
        val productExtendedPDA = ShopUtils.getProductExtendedPDA(nextProductId).getOrNull()!!
        val merchantInfoAccountPda = ShopUtils.getMerchantInfoPda(accountPublicKey).getOrNull()!!

        val createProductBaseInstruction = genTransactionInstruction(
            listOf(
                AccountMeta(accountPublicKey, true, true),
                AccountMeta(globalRootPDA, false, true),
                AccountMeta(merchantIdAccountPDA, false, true),
                AccountMeta(merchantInfoAccountPda, false, true),
                AccountMeta(getActiveChunkPDA(accountPublicKey, merchantIdAccountPDA), false, true),
                AccountMeta(paymentConfigPDA, false, false),
                AccountMeta(productBasePDA, false, true),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("create_product_base"),
                CreateProductBase(
                    product.name,
                    product.description,
                    product.price,
                    product.keywords,
                    product.stock.toULong(),
                    AppConstants.App.getMint(),
                    product.shippingFrom
                )
            )
        )

        val createExtendedIx = genTransactionInstruction(
            listOf(
                AccountMeta(accountPublicKey, true, true),
                AccountMeta(productExtendedPDA, false, true),
                AccountMeta(productBasePDA, false, true),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false),
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("create_product_extended"),
                CreateProductExtended(
                    nextProductId,
                    product.imageUrls,
                    product.shippingTo,
                    product.shippingMethods
                )
            )
        )

        val keywordsIx: List<TransactionInstruction> = genKeywordIndexInstructions(
            nextProductId,
            product.keywords,
            accountPublicKey
        )

        val priceIndexIx = genPriceInstruction(nextProductId, product.price, accountPublicKey)
        val salesIndexIx = genSalesInstruction(nextProductId, accountPublicKey)

        return listOf(createProductBaseInstruction, createExtendedIx) + keywordsIx + listOf(
            priceIndexIx,
            salesIndexIx
        )
    }

    private suspend fun genKeywordIndexInstructions(
        productId: ULong,
        keywords: List<String>,
        accountPublicKey: SolanaPublicKey
    ): List<TransactionInstruction> {
        return keywords.map { keyword ->
            runBlocking {
                val keywordRootPda = ShopUtils.getKeywordRootPda(keyword).getOrNull()!!
                val targetSharedPda = ShopUtils.getTargetShardPda(keyword).getOrNull()!!

                genTransactionInstruction(
                    listOf(
                        AccountMeta(keywordRootPda, false, true),
                        AccountMeta(targetSharedPda, false, true),
                        AccountMeta(accountPublicKey, true, true),
                        AccountMeta(SystemProgram.PROGRAM_ID, false, false)
                    ),
                    Borsh.encodeToByteArray(
                        AnchorInstructionSerializer("add_product_to_keyword_index"),
                        AddProductToKeywordIndex(keyword, productId)
                    )
                )
            }
        }
    }

    private suspend fun genPriceInstruction(
        productId: ULong,
        price: ULong,
        accountPublicKey: SolanaPublicKey
    ): TransactionInstruction {
        val priceRange = ShopUtils.calcPriceRange(price)
        val priceIndexPDA = ShopUtils.getPriceIndexPDA(priceRange).getOrNull()!!
        return genTransactionInstruction(
            listOf(
                AccountMeta(accountPublicKey, true, true),
                AccountMeta(priceIndexPDA, false, true),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("add_product_to_price_index"),
                AddProductToPriceIndex(
                    productId,
                    price,
                    priceRange.first,
                    priceRange.second,
                )
            )
        )
    }

    private suspend fun genSalesInstruction(
        productId: ULong,
        accountPublicKey: SolanaPublicKey,
        sales: UInt = 0u
    ): TransactionInstruction {
        val salesRange = ShopUtils.calcSalesRange(sales)
        val salesIndexPDA = ShopUtils.getSalesIndexPDA(salesRange).getOrNull()!!
        return genTransactionInstruction(
            listOf(
                AccountMeta(accountPublicKey, true, true),
                AccountMeta(salesIndexPDA, false, true),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("add_product_to_sales_index"),
                AddProductToSalesIndex(
                    salesRange.first,
                    salesRange.second,
                    productId,
                    sales
                )
            )
        )
    }

    private suspend fun getActiveChunkPDA(
        accountPublicKey: SolanaPublicKey,
        merchantIdAccountPDA: SolanaPublicKey
    ): SolanaPublicKey {
        val idAccount =
            networkConnectionManager.getSolanaRpcClient().getAccountInfo<MerchantIdAccount>(merchantIdAccountPDA).result?.data

        if (idAccount != null) {
            return idAccount.activeChunk
        } else {
            return ShopUtils.getInitialChunkPda(accountPublicKey).getOrNull()!!
        }
    }

    private suspend fun getNextProductId(
        accountPublicKey: SolanaPublicKey,
        merchantIdAccountPDA: SolanaPublicKey
    ): ULong {
        try {
            val activeChunkPda = getActiveChunkPDA(accountPublicKey, merchantIdAccountPDA)
            val activeChunk = networkConnectionManager.getSolanaRpcClient().getAccountInfo<IdChunk>(activeChunkPda).result?.data
            if (activeChunk != null) {
                Log.d(
                    TAG,
                    "getNextProductId : nextAvailable = ${activeChunk.nextAvailable}, startId = ${activeChunk.startId}"
                )
                return activeChunk.nextAvailable + activeChunk.startId
            } else {
                return (System.currentTimeMillis() % 90000 + 10000).toULong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getNextProductId", e)
            return (System.currentTimeMillis() % 90000 + 10000).toULong()
        }
    }

    override suspend fun updateProduct(
        product: Product,
        accountPublicKey: String,
        activityResultSender: ActivityResultSender
    ) {
        try {
            Log.d(TAG, "Starting updateProduct for product: ${product.name}")
            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(
                    TAG, "updateProduct authResult.authToken:${authResult.authToken}"
                )

                val builder = Message.Builder()
                val instruction = genUpdateProductInstruction(product, accountPublicKey)

                builder.addInstruction(instruction)
                val recentBlockhash = try {
                    withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }
                val message = builder.setRecentBlockhash(recentBlockhash).build()

                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (updateProduct): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (updateProduct): $signResult")
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(
                            TAG,
                            "Product updated successfully: $signatureString"
                        )
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(networkConnectionManager.getSolanaRpcClient(), signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d(TAG, "Transaction confirmed: $signatureString")
                            // Update local list only after successful blockchain transaction
                            val index = products.indexOfFirst { it.id == product.id }
                            if (index != -1) {
                                products[index] = product
                            }
                        } else {
                            Log.e(TAG, "Transaction confirmation failed: $signatureString")
                            throw Exception("Transaction confirmation failed")
                        }
                    } else {
                        Log.e(TAG, "No signature returned from updateProduct transaction")
                        throw Exception("No signature returned from transaction")
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for updateProduct")
                    throw Exception("No wallet found")
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "updateProduct failed: ${result.e.message}")
                    throw Exception("Transaction failed: ${result.e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateProduct exception:", e)
            throw Exception("Failed to update product: ${e.message}")
        }
    }

    override suspend fun deleteProduct(
        productId: ULong,
        accountPublicKey: String,
        activityResultSender: ActivityResultSender
    ) {
        try {
            Log.d(TAG, "Starting deleteProduct for productId: $productId")
            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(
                    TAG, "deleteProduct authResult.authToken:${authResult.authToken}"
                )

                val builder = Message.Builder()
                val instruction = genDeleteProductInstruction(
                    productId,
                    SolanaPublicKey.from(accountPublicKey)
                )
                builder.addInstruction(instruction)
                val recentBlockhash = try {
                    withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }
                val message = builder.setRecentBlockhash(recentBlockhash).build()

                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (deleteProduct): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (deleteProduct): $signResult")
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(
                            TAG,
                            "Product deleted successfully: $signatureString"
                        )
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(networkConnectionManager.getSolanaRpcClient(), signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d(TAG, "Transaction confirmed: $signatureString")
                            // Remove from local list only after successful blockchain transaction
                            products.removeAll { it.id == productId }
                        } else {
                            Log.e(TAG, "Transaction confirmation failed: $signatureString")
                            throw Exception("Transaction confirmation failed")
                        }
                    } else {
                        Log.e(TAG, "No signature returned from deleteProduct transaction")
                        throw Exception("No signature returned from transaction")
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for deleteProduct")
                    throw Exception("No wallet found")
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "deleteProduct failed: ${result.e.message}")
                    throw Exception("Transaction failed: ${result.e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteProduct exception:", e)
            throw Exception("Failed to delete product: ${e.message}")
        }
    }

    private suspend fun genUpdateProductInstruction(
        product: Product,
        accountPublicKey: String
    ): TransactionInstruction {
        val accountPublicKey = SolanaPublicKey.from(accountPublicKey)
        val productBasePDA = ShopUtils.getProductBasePDA(product.id).getOrNull()!!
        val productExtendedPDA = ShopUtils.getProductExtendedPDA(product.id).getOrNull()!!


        return genTransactionInstruction(
            listOf(
                AccountMeta(accountPublicKey, true, true),
                AccountMeta(productBasePDA, false, true),
                AccountMeta(productExtendedPDA, false, true),
                AccountMeta(ShopUtils.getPaymentPda().getOrNull()!!, false, false),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("update_product"),
                UpdateProduct(
                    productId = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    keywords = product.keywords,
                    inventory = product.stock.toULong(),
                    paymentToken = AppConstants.App.getMint(),
                    imageVideoUrls = product.imageUrls,
                    shippingLocation = product.shippingFrom,
                    salesRegions = product.shippingTo,
                    logisticsMethods = product.shippingMethods
                )
            )
        )
    }


    private suspend fun genDeleteProductInstruction(
        productId: ULong,
        accountPublicKey: SolanaPublicKey
    ): TransactionInstruction {
        return genTransactionInstruction(
            listOf(
                AccountMeta(accountPublicKey, true, true),
                AccountMeta(
                    ShopUtils.getMerchantInfoPda(accountPublicKey).getOrNull()!!,
                    false,
                    true
                ),
                AccountMeta(
                    ShopUtils.getProductBasePDA(productId).getOrNull()!!,
                    false,
                    true
                ),
                AccountMeta(accountPublicKey, true, true),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("delete_product"),
                DeleteProduct(
                    productId = productId,
                    hardDelete = true,
                    force = false
                )
            )
        )
    }

    override fun clearRecommendCache() {
        recommendProductCacheDataSource.clearCache()
    }

    override fun hasValidRecommendCache(): Boolean {
        return recommendProductCacheDataSource.hasValidCache()
    }

}