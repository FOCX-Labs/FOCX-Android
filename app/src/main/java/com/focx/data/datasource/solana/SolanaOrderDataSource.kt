package com.focx.data.datasource.solana

import com.focx.core.constants.AppConstants
import com.focx.domain.entity.CreateOrder
import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderManagementStatus
import com.focx.domain.entity.OrderPayment
import com.focx.domain.entity.Product
import com.focx.domain.entity.UserPurchaseCount
import com.focx.domain.repository.IOrderRepository
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.focx.core.network.NetworkConnectionManager
import com.focx.utils.Utils
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
import com.solana.transaction.Message.Builder
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaOrderDataSource @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val networkConnectionManager: NetworkConnectionManager
) : IOrderRepository {

    companion object {
        const val TAG = "SolanaOrderDataSource"
    }

    override suspend fun getOrders(): Flow<List<Order>> = flow {
        emit(emptyList()) // TODO: 实现链上订单查询
    }

    override suspend fun getOrderById(id: String): Order? {
        return try {
            Log.d("SolanaOrder", "Fetching order by ID: $id")
            val orderPda = SolanaPublicKey.from(id)
            ShopUtils.getOrderInfoByPda(orderPda, networkConnectionManager.getSolanaRpcClient())
        } catch (e: Exception) {
            Log.e("SolanaOrder", "Error fetching order by ID: $id", e)
            null
        }
    }

    override suspend fun getOrdersByBuyer(buyerId: String): Flow<List<Order>> = flow {
        emit(ShopUtils.getOrdersByBuyer(buyerId, networkConnectionManager.getSolanaRpcClient()))
    }

    override suspend fun getOrdersBySeller(sellerId: String): Flow<List<Order>> = flow {
        emit(ShopUtils.getOrdersBySeller(sellerId, networkConnectionManager.getSolanaRpcClient()))
    }

    override suspend fun getOrdersByStatus(status: OrderManagementStatus): Flow<List<Order>> =
        flow {
            emit(emptyList()) // TODO: 实现链上订单查询
        }

    override suspend fun createOrder(
        product: Product,
        quantity: UInt,
        buyer: String,
        shippingAddress: com.focx.domain.entity.ShippingAddress,
        orderNote: String,
        activityResultSender: ActivityResultSender
    ): Result<Order> {
        return try {
            Log.d(
                "SolanaOrder",
                "Starting createOrder for product: ${product.id}, buyer: $buyer, quantity: $quantity"
            )
            val buyerPubkey = SolanaPublicKey.from(buyer)
            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val builder = Builder()
                val instructions = genCreateOrderInstructions(
                    product,
                    quantity,
                    buyerPubkey,
                    shippingAddress,
                    orderNote
                )
                instructions.forEach { ix -> builder.addInstruction(ix) }
                val recentBlockhash = recentBlockhashUseCase()
                val message = builder.setRecentBlockhash(recentBlockhash).build()
                val transaction = Transaction(message)
                val signResult = signAndSendTransactions(arrayOf(transaction.serialize()))
                signResult
            }
            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d("SolanaOrder", "Order created successfully: $signatureString")
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(networkConnectionManager.getSolanaRpcClient(), signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d("SolanaOrder", "Transaction confirmed: $signatureString")
                            Result.success(
                                Order(
                                    id = java.util.UUID.randomUUID().toString(),
                                    buyerId = buyer,
                                    sellerId = product.sellerId,
                                    sellerName = product.sellerName,
                                    items = listOf(
                                        com.focx.domain.entity.OrderItem(
                                            id = java.util.UUID.randomUUID().toString(),
                                            productId = product.id.toString(),
                                            productName = product.name,
                                            productImage = product.imageUrls.firstOrNull() ?: "",
                                            quantity = quantity.toInt(),
                                            unitPrice = product.price.toDouble(),
                                            totalPrice = product.price.toDouble() * quantity.toDouble()
                                        )
                                    ),
                                    totalAmount = product.price.toDouble() * quantity.toDouble(),
                                    currency = product.currency,
                                    status = OrderManagementStatus.Pending,
                                    shippingAddress = shippingAddress,
                                    orderNote = orderNote,
                                    paymentMethod = "USDC",
                                    transactionHash = signatureString
                                )
                            )
                        } else {
                            Log.e("SolanaOrder", "Transaction confirmation failed: $signatureString")
                            Result.failure(Exception("Transaction confirmation failed"))
                        }
                    } else {
                        Log.e("SolanaOrder", "No signature returned from createOrder transaction")
                        Result.failure(Exception("No signature returned from transaction"))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e("SolanaOrder", "No wallet found for createOrder")
                    Result.failure(Exception("No wallet found"))
                }

                is TransactionResult.Failure -> {
                    Log.e("SolanaOrder", "createOrder failed: ${result.e.message}")
                    Result.failure(Exception("Transaction failed: ${result.e.message}"))
                }

                else -> Result.failure(Exception("Unknown transaction result"))
            }
        } catch (e: Exception) {
            Log.e("SolanaOrder", "createOrder exception:", e)
            Result.failure(Exception("Failed to create order: ${e.message}"))
        }
    }

    private suspend fun genCreateOrderInstructions(
        product: Product,
        quantity: UInt,
        buyerPubkey: SolanaPublicKey,
        shippingAddress: com.focx.domain.entity.ShippingAddress,
        orderNote: String
    ): List<TransactionInstruction> {
        val merchantPubKey = SolanaPublicKey.from(product.sellerId)
        val userPurchaseCountPda = ShopUtils.getUserPurchaseCountPDA(buyerPubkey).getOrNull()!!

        val currentPurchaseCount = getCurrentPurchaseCount(userPurchaseCountPda)
        val merchantPda = ShopUtils.getMerchantInfoPda(merchantPubKey).getOrNull()!!
        val orderPda = ShopUtils.getOrderPda(buyerPubkey, currentPurchaseCount + 1UL).getOrNull()!!
        val orderStatsPda = ShopUtils.getOrderStatsPda().getOrNull()!!
        val productPda = ShopUtils.getProductBasePDA(product.id).getOrNull()!!
        val merchantOrderCountPDA = ShopUtils.getMerchantOrderCountPDA(merchantPubKey).getOrNull()!!
        val merchantOrderCount =
            ShopUtils.getMerchantOrderCount(merchantOrderCountPDA, networkConnectionManager.getSolanaRpcClient())
        val nextMerchantOrderPDA =
            ShopUtils.getMerchantOrderPDA(merchantPubKey, merchantOrderCount + 1UL).getOrNull()!!


        val createIx = ShopUtils.genTransactionInstruction(
            listOf(
                AccountMeta(userPurchaseCountPda, false, true),
                AccountMeta(orderPda, false, true),
                AccountMeta(orderStatsPda, false, true),
                AccountMeta(productPda, false, false),
                AccountMeta(merchantPda, false, false),
                AccountMeta(merchantOrderCountPDA, false, true),
                AccountMeta(nextMerchantOrderPDA, false, true),
                AccountMeta(buyerPubkey, true, true),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("create_order"),
                CreateOrder(
                    product.id,
                    quantity,
                    "${shippingAddress.recipientName}, ${shippingAddress.addressLine1}, ${shippingAddress.city}, ${shippingAddress.state} ${shippingAddress.postalCode}, ${shippingAddress.phoneNumber}",
                    orderNote,
                    "atomic_purchase_tx"
                )
            )
        )

        val programTokenAccountPDA = ShopUtils.getProgramTokenAccountPda().getOrNull()!!
        val programAuthorityPDA = ShopUtils.getSimplePda("program_authority").getOrNull()!!
        val buyerTokenAccount = ShopUtils.getAssociatedTokenAddress(buyerPubkey).getOrNull()!!

        val payIx = ShopUtils.genTransactionInstruction(
            listOf(
                AccountMeta(buyerPubkey, true, true),
                AccountMeta(productPda, false, false),
                AccountMeta(programTokenAccountPDA, false, true),
                AccountMeta(programAuthorityPDA, false, false),
                AccountMeta(buyerTokenAccount, false, true),
                AccountMeta(AppConstants.App.getMint(), false, false),
                AccountMeta(
                    SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID),
                    false,
                    false
                ),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("purchase_product_escrow"),
                OrderPayment(
                    product.id,
                    quantity.toULong()
                )
            )
        )

        return listOf(createIx, payIx)
    }

    private suspend fun getCurrentPurchaseCount(userPurchaseCountPda: SolanaPublicKey): ULong {
        return try {
            val userPurchaseCountAccount =
                networkConnectionManager.getSolanaRpcClient().getAccountInfo<UserPurchaseCount>(userPurchaseCountPda).result?.data
            if (userPurchaseCountAccount !== null) {
                userPurchaseCountAccount.purchaseCount
            } else {
                0UL
            }
        } catch (e: Exception) {
            0UL
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        status: OrderManagementStatus
    ): Result<Unit> {
        return try {
            Log.d("SolanaOrder", "Updating order status: $orderId to $status")
            // TODO: 实现链上订单状态更新
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SolanaOrder", "Error updating order status", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> {
        return try {
            Log.d("SolanaOrder", "Cancelling order: $orderId")
            // TODO: 实现链上订单取消
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SolanaOrder", "Error cancelling order", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTrackingNumber(
        orderId: String,
        trackingNumber: String,
        merchantPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit> {
        return try {
            Log.d(
                "SolanaOrder",
                "Starting updateTrackingNumber for order: $orderId, trackingNumber: $trackingNumber"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val builder = Builder()

                // Generate update tracking number instruction
                val merchantInfoPDA = ShopUtils.getMerchantInfoPda(merchantPubKey).getOrNull()!!
                val orderPda = SolanaPublicKey.from(orderId)
                val orderStats = ShopUtils.getOrderStatsPda().getOrNull()!!

                val ix = ShopUtils.genTransactionInstruction(
                    listOf(
                        AccountMeta(orderPda, false, true),
                        AccountMeta(orderStats, false, true),
                        AccountMeta(merchantInfoPDA, false, true),
                        AccountMeta(merchantPubKey, true, true),
                    ),
                    Borsh.encodeToByteArray(
                        AnchorInstructionSerializer("ship_order"),
                        trackingNumber.toByteArray()
                    )
                )

                builder.addInstruction(ix)

                // Get recent blockhash and build message
                val recentBlockhash = recentBlockhashUseCase()
                val message = builder.setRecentBlockhash(recentBlockhash).build()

                // Create and sign transaction
                val transaction = Transaction(message)
                val signResult = signAndSendTransactions(arrayOf(transaction.serialize()))
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d("SolanaOrder", "Tracking number updated successfully: $signatureString")
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(networkConnectionManager.getSolanaRpcClient(), signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d("SolanaOrder", "Transaction confirmed: $signatureString")
                            Result.success(Unit)
                        } else {
                            Log.e("SolanaOrder", "Transaction confirmation failed: $signatureString")
                            Result.failure(Exception("Transaction confirmation failed"))
                        }
                    } else {
                        Log.e(
                            "SolanaOrder",
                            "No signature returned from updateTrackingNumber transaction"
                        )
                        Result.failure(Exception("No signature returned from transaction"))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e("SolanaOrder", "No wallet found for updateTrackingNumber")
                    Result.failure(Exception("No wallet found"))
                }

                is TransactionResult.Failure -> {
                    Log.e("SolanaOrder", "updateTrackingNumber failed: ${result.e.message}")
                    Result.failure(Exception("Transaction failed: ${result.e.message}"))
                }

                else -> Result.failure(Exception("Unknown transaction result"))
            }
        } catch (e: Exception) {
            Log.e("SolanaOrder", "updateTrackingNumber exception:", e)
            Result.failure(Exception("Failed to update tracking number: ${e.message}"))
        }
    }

    override suspend fun confirmReceipt(
        orderId: String,
        buyerPubKey: SolanaPublicKey,
        merchantPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit> {
        return try {
            Log.d("SolanaOrder", "Starting confirmReceipt for order: $orderId")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val builder = Builder()

                // Generate confirm receipt instruction
                val orderPda = SolanaPublicKey.from(orderId)
                val orderStats = ShopUtils.getOrderStatsPda().getOrNull()!!
                val programTokenAccountPDA = ShopUtils.getProgramTokenAccountPda().getOrNull()!!
                val programAuthorityPDA = ShopUtils.getSimplePda("program_authority").getOrNull()!!
                val systemConfigPDA = ShopUtils.getSystemConfigPDA().getOrNull()!!
                val depositEscrowPda = ShopUtils.getDepositEscrowPda().getOrNull()!!
                val merchantInfoPda = ShopUtils.getMerchantInfoPda(merchantPubKey).getOrNull()!!
                val systemConfig = ShopUtils.getSystemConfig(networkConnectionManager.getSolanaRpcClient())

                val ix = ShopUtils.genTransactionInstruction(
                    listOf(
                        AccountMeta(orderPda, false, true),
                        AccountMeta(orderStats, false, true),
                        AccountMeta(merchantInfoPda, false, true),
                        AccountMeta(systemConfigPDA, false, false),
                        AccountMeta(programTokenAccountPDA, false, true),
                        AccountMeta(depositEscrowPda, false, true),
                        AccountMeta(programAuthorityPDA, false, false),
                        AccountMeta(systemConfig.vaultAccount, false, true),
                        AccountMeta(systemConfig.vaultTokenAccount, false, true),
                        AccountMeta(systemConfig.platformTokenAccount, false, true),
                        AccountMeta(systemConfig.vaultProgramId, false, true),
                        AccountMeta(buyerPubKey, true, true),
                        AccountMeta(
                            SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID),
                            false,
                            false
                        ),
                        AccountMeta(SystemProgram.PROGRAM_ID, false, false)
                    ),
                    Borsh.encodeToByteArray(
                        AnchorInstructionSerializer("confirm_delivery"),
                        Unit
                    )
                )

                builder.addInstruction(ix)

                // Get recent blockhash and build message
                val recentBlockhash = recentBlockhashUseCase()
                val message = builder.setRecentBlockhash(recentBlockhash).build()

                // Create and sign transaction
                val transaction = Transaction(message)
                val signResult = signAndSendTransactions(arrayOf(transaction.serialize()))
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d("SolanaOrder", "Receipt confirmed successfully: $signatureString")
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(networkConnectionManager.getSolanaRpcClient(), signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d("SolanaOrder", "Transaction confirmed: $signatureString")
                            Result.success(Unit)
                        } else {
                            Log.e("SolanaOrder", "Transaction confirmation failed: $signatureString")
                            Result.failure(Exception("Transaction confirmation failed"))
                        }
                    } else {
                        Log.e(
                            "SolanaOrder",
                            "No signature returned from confirmReceipt transaction"
                        )
                        Result.failure(Exception("No signature returned from transaction"))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e("SolanaOrder", "No wallet found for confirmReceipt")
                    Result.failure(Exception("No wallet found"))
                }

                is TransactionResult.Failure -> {
                    Log.e("SolanaOrder", "confirmReceipt failed: ${result.e.message}")
                    Result.failure(Exception("Transaction failed: ${result.e.message}"))
                }

                else -> Result.failure(Exception("Unknown transaction result"))
            }
        } catch (e: Exception) {
            Log.e("SolanaOrder", "confirmReceipt exception:", e)
            Result.failure(Exception("Failed to confirm receipt: ${e.message}"))
        }
    }

}