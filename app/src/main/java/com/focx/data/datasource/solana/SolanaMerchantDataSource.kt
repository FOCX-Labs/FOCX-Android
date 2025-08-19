package com.focx.data.datasource.solana

import android.content.Context
import com.focx.core.constants.AppConstants
import com.focx.core.constants.AppConstants.App.SPL_TOKEN_PROGRAM_ID
import com.focx.core.constants.AppConstants.Merchant.DEFAULT_STATUS
import com.focx.core.network.NetworkConfig
import com.focx.domain.entity.Merchant
import com.focx.domain.entity.MerchantRegistration
import com.focx.domain.entity.MerchantRegistrationResult
import com.focx.domain.entity.MerchantStatus
import com.focx.domain.repository.IMerchantRepository
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.DebugUtils
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.focx.utils.Utils
import com.focx.utils.ShopUtils.genTransactionInstruction
import com.focx.utils.ShopUtils.getAssociatedTokenAddress
import com.focx.utils.ShopUtils.getDepositEscrowPda
import com.focx.utils.ShopUtils.getGlobalRootPda
import com.focx.utils.ShopUtils.getInitialChunkPda
import com.focx.utils.ShopUtils.getMerchantIdPda
import com.focx.utils.ShopUtils.getMerchantInfoPda
import com.focx.utils.ShopUtils.getSystemConfigPDA
import com.funkatronics.encoders.Base58
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
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RegisterMerchantAtomicArgs(
    val name: String,
    val description: String
)

@Singleton
class SolanaMerchantDataSource @Inject constructor(
    private val context: Context,
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val solanaRpcClient: SolanaRpcClient
) : IMerchantRepository {

    companion object {
        private const val TAG = "SMDS"
    }

    override suspend fun registerMerchantAtomic(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(
                TAG,
                "Starting registerMerchantWithAnchor, walletAdapter authToken: ${walletAdapter.authToken}"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(
                    TAG, "registerMerchantWithAnchor authResult.authToken:${authResult.authToken}"
                )

                val registerInstruction =
                    genRegisterInstruction(merchantRegistration, activityResultSender)
                val depositInstruction =
                    genDepositMerchantDepositInstruction(
                        merchantRegistration.merchantPublicKey,
                        merchantRegistration.securityDeposit)

                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }
                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder()
                    .addInstruction(registerInstruction)
                    .addInstruction(depositInstruction)
                    .setRecentBlockhash(recentBlockhash)
                    .build()
                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (Anchor): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (Anchor): $signResult")
                signResult
            }


            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(
                            TAG,
                            "Anchor registration successful: $signatureString"
                        )
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(solanaRpcClient, signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d(TAG, "Transaction confirmed: $signatureString")
                            MerchantRegistrationResult(
                                success = true,
                                transactionSignature = signatureString,
                                merchantAccount = merchantRegistration.merchantPublicKey
                            )
                        } else {
                            Log.e(TAG, "Transaction confirmation failed: $signatureString")
                            MerchantRegistrationResult(
                                success = false, errorMessage = "Transaction confirmation failed"
                            )
                        }
                    } else {
                        MerchantRegistrationResult(
                            success = false, errorMessage = "No signature returned from transaction"
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false, errorMessage = "No wallet found"
                    )
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Anchor registration failed: ${result.e}")
                    MerchantRegistrationResult(
                        success = false, errorMessage = "Transaction failed: ${result.e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Anchor registration exception:", e)
            MerchantRegistrationResult(
                success = false, errorMessage = "Registration failed: ${e.message}"
            )
        }
    }

    private suspend fun genRegisterInstruction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): TransactionInstruction {
        Log.d(TAG, "genRegisterTransaction start")

        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)


        val globalRootPda = getGlobalRootPda()
        val merchantInfoPda = getMerchantInfoPda(merchantPublicKey)
        val systemConfigPda = getSystemConfigPDA()
        val merchantIdAccountPda = getMerchantIdPda(merchantPublicKey)
        val initialChunkPda = getInitialChunkPda(merchantPublicKey)

        val args = RegisterMerchantAtomicArgs(
            name = merchantRegistration.name, description = merchantRegistration.description
        )

        val instructionData = Borsh.encodeToByteArray(AnchorInstructionSerializer("register_merchant_atomic"), args)

        val accountMetas = listOf(
            AccountMeta(
                merchantPublicKey, true, true
            ), AccountMeta(merchantPublicKey, true, true), AccountMeta(
                globalRootPda.getOrNull()!!, false, true
            ), AccountMeta(
                merchantInfoPda.getOrNull()!!, false, true
            ), AccountMeta(
                systemConfigPda.getOrNull()!!, false, false
            ), AccountMeta(
                merchantIdAccountPda.getOrNull()!!, false, true
            ), AccountMeta(
                initialChunkPda.getOrNull()!!, false, true
            ), AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )


        return genTransactionInstruction( accountMetas, instructionData)
    }

    private suspend fun genDepositMerchantDepositInstruction(
        merchantAccount: String,
        depositAmount: ULong
    ): TransactionInstruction {
        Log.d(TAG, "genDepositMerchantDepositTransaction start")

        val merchantPublicKey = SolanaPublicKey.from(merchantAccount)


        val instructionData = Borsh.encodeToByteArray(
            AnchorInstructionSerializer("manage_deposit"),
            depositAmount)

        val merchantInfoPda = getMerchantInfoPda(merchantPublicKey)
        val systemConfigPda = getSystemConfigPDA()
        val depositEscrowPda = getDepositEscrowPda()

        val merchantTokenAccount = getAssociatedTokenAddress( merchantPublicKey)

        val accountMetas = listOf(
            AccountMeta(merchantPublicKey, true, true),
            AccountMeta(merchantInfoPda.getOrNull()!!, false, true),
            AccountMeta(systemConfigPda.getOrNull()!!, false, false),
            AccountMeta(merchantTokenAccount.getOrNull()!!, false, true),
            AccountMeta(AppConstants.App.getMint(), false, false),
            AccountMeta(depositEscrowPda.getOrNull()!!, false, true),
            AccountMeta(SolanaPublicKey.from(SPL_TOKEN_PROGRAM_ID), false, false),
            AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )

        return genTransactionInstruction(accountMetas, instructionData)
    }


    override suspend fun registerMerchantAtomic(
        merchantRegistration: MerchantRegistration
    ): MerchantRegistrationResult {
        return MerchantRegistrationResult(
            success = false,
            transactionSignature = null,
            merchantAccount = null,
            errorMessage = "ActivityResultSender is required for wallet transactions. Use the overloaded method."
        )
    }

    override suspend fun getMerchantAccountData(walletAddress: String): Flow<MerchantStatus> =
        flow {
            try {
                val merchantPublicKey = SolanaPublicKey.from(walletAddress)
                val pda = getMerchantInfoPda(SolanaPublicKey.from(walletAddress))
                Log.d(TAG, "merchant pad: ${pda.getOrNull()!!.base58()}")
                val merchant = solanaRpcClient.getAccountInfo<Merchant>(pda.getOrNull()!!).result
                val orderCount = ShopUtils.getMerchantOrderCount(ShopUtils.getMerchantOrderCountPDA(merchantPublicKey).getOrNull()!!, solanaRpcClient)

                if (!(merchant == null || merchant.data == null)) {
                    Log.d(TAG, "merchant info: ${merchant.data!!.name}, ${merchant.data!!.description}, ${merchant.data!!.depositAmount},")

                    val merchantStatus = MerchantStatus(
                        isRegistered = true,
                        merchantAccount = walletAddress,
                        orderCounts = orderCount,
                        productCounts = merchant.data!!.productCount,
                        registrationDate = merchant.data!!.createdAt.toString(),
                        securityDeposit = merchant.data!!.depositAmount,
                        status = DEFAULT_STATUS
                    )
                    Log.d(TAG, "Merchant account found and parsed successfully")
                    emit(merchantStatus)
                } else {
                    Log.d(TAG, "Merchant account not found or not initialized")
                    emit(
                        MerchantStatus(
                            isRegistered = false,
                            merchantAccount = null,
                            registrationDate = null,
                            securityDeposit = null,
                            status = DEFAULT_STATUS
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying merchant account data: ${e.message}", e)
//                if (AbortFlowException.)
//                emit(
//                    MerchantStatus(
//                        isRegistered = false,
//                        merchantAccount = null,
//                        registrationDate = null,
//                        securityDeposit = null,
//                        status = AppConstants.Merchant.DEFAULT_STATUS
//                    )
//                )
            }
        }

    override suspend fun depositMerchantFunds(
        merchantAccount: String, depositAmount: ULong, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(
                TAG,
                "Starting depositMerchantFunds for merchant: $merchantAccount, amount: $depositAmount"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val depositInstruction =
                    genDepositMerchantDepositInstruction(
                        merchantAccount,
                        depositAmount)

                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }
                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder()
                    .addInstruction(depositInstruction)
                    .setRecentBlockhash(recentBlockhash)
                    .build()
                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (Anchor): before")
                val result = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (Anchor): $result")
                result
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(TAG, "Deposit successful: $signatureString")
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(solanaRpcClient, signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d(TAG, "Transaction confirmed: $signatureString")
                            MerchantRegistrationResult(
                                success = true,
                                transactionSignature = signatureString,
                                merchantAccount = merchantAccount,
                                errorMessage = null
                            )
                        } else {
                            Log.e(TAG, "Transaction confirmation failed: $signatureString")
                            MerchantRegistrationResult(
                                success = false,
                                transactionSignature = null,
                                merchantAccount = null,
                                errorMessage = "Transaction confirmation failed"
                            )
                        }
                    } else {
                        MerchantRegistrationResult(
                            success = false,
                            transactionSignature = null,
                            merchantAccount = null,
                            errorMessage = "No signature returned from deposit transaction"
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "No wallet found for deposit"
                    )
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Deposit failed: ${result.e.message}")
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "Deposit transaction failed: ${result.e.message}"
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Deposit exception:", e)
            MerchantRegistrationResult(
                success = false,
                transactionSignature = null,
                merchantAccount = null,
                errorMessage = "Deposit failed: ${e.message}"
            )
        }
    }
}