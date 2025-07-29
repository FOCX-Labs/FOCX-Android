package com.focx.data.datasource.solana

import android.content.Context
import com.focx.core.constants.AppConstants
import com.focx.domain.entity.CreateOrder
import com.focx.domain.entity.Order
import com.focx.domain.entity.OrderPayment
import com.focx.domain.entity.Product
import com.focx.domain.entity.UserPurchaseCount
import com.focx.domain.repository.IOrderRepository
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.funkatronics.kborsh.Borsh
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.programs.SystemProgram
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.getAccountInfo
import com.solana.serialization.AnchorInstructionSerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message.Builder
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SolanaOrderDataSource @Inject constructor(
    private val context: Context,
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val solanaRpcClient: SolanaRpcClient
) : IOrderRepository {
    override suspend fun getOrders(): Flow<List<Order>> = flow {
        emit(emptyList()) // TODO: 实现链上订单查询
    }

    override suspend fun getOrderById(id: String): Order? {
        return null // TODO: 实现链上订单查询
    }

    override suspend fun getOrdersByBuyer(buyerId: String): Flow<List<Order>> = flow {
        emit(emptyList()) // TODO: 实现链上订单查询
    }

    override suspend fun getOrdersBySeller(sellerId: String): Flow<List<Order>> = flow {
        emit(emptyList()) // TODO: 实现链上订单查询
    }

    override suspend fun getOrdersByStatus(status: String): Flow<List<Order>> = flow {
        emit(emptyList()) // TODO: 实现链上订单查询
    }

    override suspend fun createOrder(product: Product, quantity: UInt, buyer: String, activityResultSender: ActivityResultSender): Result<Order> {
        return try {
            Log.d("SolanaOrder", "Starting createOrder for product: ${product.id}, buyer: $buyer, quantity: $quantity")
            val buyerPubkey = SolanaPublicKey.from(buyer)
            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val builder = Builder()
                val instructions = genCreateOrderInstructions(product, quantity, buyerPubkey)
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
                        Log.d("SolanaOrder", "Order created successfully: $signature")
                        Result.success(Order(
                            id = java.util.UUID.randomUUID().toString(),
                            buyerId = buyer,
                            sellerId = product.sellerId,
                            sellerName = product.sellerName,
                            items = listOf(com.focx.domain.entity.OrderItem(
                                id = java.util.UUID.randomUUID().toString(),
                                productId = product.id.toString(),
                                productName = product.name,
                                productImage = product.imageUrls.firstOrNull() ?: "",
                                quantity = quantity.toInt(),
                                unitPrice = product.price.toDouble(),
                                totalPrice = product.price.toDouble() * quantity.toDouble()
                            )),
                            totalAmount = product.price.toDouble() * quantity.toDouble(),
                            currency = product.currency,
                            status = "PENDING_PAYMENT",
                            paymentMethod = "USDC",
                            transactionHash = signature.toString()
                        ))
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

    private suspend fun genCreateOrderInstructions(product: Product, quantity: UInt, buyerPubkey: SolanaPublicKey): List<TransactionInstruction> {
        val merchantPubKey = SolanaPublicKey.from(product.sellerId)
        val userPurchaseCountPda = ShopUtils.getUserPurchaseCountPDA(buyerPubkey).getOrNull()!!

        val currentPurchaseCount = getCurrentPurchaseCount(userPurchaseCountPda)
        val merchantPda = ShopUtils.getMerchantInfoPda(merchantPubKey).getOrNull()!!
        val orderPda = ShopUtils.getOrderPda(merchantPda, buyerPubkey, product.id, currentPurchaseCount).getOrNull()!!
        val orderStatsPda = ShopUtils.getOrderStatsPda().getOrNull()!!
        val productPda = ShopUtils.getProductBasePDA(product.id).getOrNull()!!


        val createIx = ShopUtils.genTransactionInstruction(
            listOf(
                AccountMeta(userPurchaseCountPda, false, true),
                AccountMeta(orderPda, false, true),
                AccountMeta(orderStatsPda, false, true),
                AccountMeta(productPda, false, false),
                AccountMeta(merchantPda, false, false),
                AccountMeta(buyerPubkey, true, true),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false),

            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("create_order"),
                CreateOrder(
                    product.id,
                    quantity,
                    "shipping address",
                    "default notes",
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
                AccountMeta(SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID), false, false),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("purchase_product_escrow"),
                OrderPayment(
                    product.id,
                    quantity
                )
            )
        )

        return listOf(createIx, payIx)
    }

    private suspend fun getCurrentPurchaseCount(userPurchaseCountPda: SolanaPublicKey): ULong {
        return try {
            val userPurchaseCountAccount =  solanaRpcClient.getAccountInfo<UserPurchaseCount>(userPurchaseCountPda).result?.data
            if (userPurchaseCountAccount !== null) {
                userPurchaseCountAccount.purchaseCount
            } else {
                0UL
            }
        } catch (e: Exception) {
            0UL
        }

    }

    override suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        // TODO: 实现链上订单状态更新
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun cancelOrder(orderId: String): Result<Unit> {
        // TODO: 实现链上订单取消
        return Result.failure(Exception("Not implemented"))
    }

    override suspend fun updateTrackingNumber(orderId: String, trackingNumber: String): Result<Unit> {
        // TODO: 实现链上物流单号更新
        return Result.failure(Exception("Not implemented"))
    }
}