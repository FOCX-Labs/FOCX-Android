package com.focx.data.datasource.solana

import android.content.Context
import com.focx.core.constants.AppConstants
import com.focx.core.network.NetworkConfig
import com.focx.domain.entity.StakeActivity

import com.focx.domain.entity.Vault
import com.focx.domain.entity.VaultDepositor
import com.focx.domain.entity.VaultInfoWithStakers
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.focx.utils.Utils
import com.focx.utils.ShopUtils.genTransactionInstruction
import com.focx.utils.VaultUtils
import com.funkatronics.encoders.Base58
import com.funkatronics.kborsh.Borsh
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.programs.SystemProgram
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.AccountRequest
import com.solana.rpc.Commitment
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.getAccountInfo
import com.solana.serialization.AnchorInstructionSerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import javax.inject.Inject

class SolanaVaultDataSource @Inject constructor(
    private val context: Context,
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val solanaRpcClient: SolanaRpcClient
) {
    companion object {
        private const val TAG = "SVDS"
    }

    suspend fun getVaultInfo(accountPublicKey: String): Flow<Result<Vault>> = flow {
        try {
            Log.d(TAG, "Getting vault info for account: $accountPublicKey")

            val vault = getVaultInfoFromChain(accountPublicKey)
            emit(Result.success(vault))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get vault info: ${e.message}", e)
            emit(Result.failure(Exception("Failed to get vault info: ${e.message}")))
        }
    }

    suspend fun getStakingInfo(accountPublicKey: String): Flow<Result<VaultDepositor?>> = flow {
        try {
            Log.d(TAG, "Getting staking info for account: $accountPublicKey")

            // Get staking info from blockchain
            val vaultDepositor = getStakingInfoFromChain(accountPublicKey)
            emit(Result.success(vaultDepositor))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get staking info: ${e.message}", e)
            emit(Result.failure(Exception("Failed to get staking info: ${e.message}")))
        }
    }

    suspend fun getStakeActivities(accountPublicKey: String): Flow<Result<List<StakeActivity>>> =
        flow {
            try {
                Log.d(TAG, "Getting stake activities for account: $accountPublicKey")

                // Get stake activities from blockchain
                val activities = getStakeActivitiesFromChain(accountPublicKey)
                emit(Result.success(activities))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get stake activities: ${e.message}", e)
                emit(Result.failure(Exception("Failed to get stake activities: ${e.message}")))
            }
        }

    suspend fun stakeUsdc(
        accountPublicKey: String,
        amount: ULong,
        activityResultSender: ActivityResultSender
    ): Flow<Result<String>> = flow {
        try {
            Log.d(TAG, "Staking USDC for account: $accountPublicKey, amount: $amount")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "Stake USDC authResult.authToken: ${authResult.authToken}")

                val builder = Message.Builder()
                val instructions = genStakeInstructions(accountPublicKey, amount)

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

                Log.d(TAG, "signAndSendTransactions (stake): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (stake): $signResult")
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(TAG, "Stake USDC successful: $signatureString")
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(solanaRpcClient, signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d(TAG, "Transaction confirmed: $signatureString")
                            emit(Result.success(signatureString))
                        } else {
                            Log.e(TAG, "Transaction confirmation failed: $signatureString")
                            emit(Result.failure(Exception("Transaction confirmation failed")))
                        }
                    } else {
                        Log.e(TAG, "No signature returned from stake transaction")
                        emit(Result.failure(Exception("No signature returned from transaction")))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for stake")
                    emit(Result.failure(Exception("No wallet found")))
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Stake failed: ${result.e.message}")
                    emit(Result.failure(Exception("Transaction failed: ${result.e.message}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stake exception:", e)
            emit(Result.failure(Exception("Failed to stake USDC: ${e.message}")))
        }
    }

    suspend fun requestUnstake(
        accountPublicKey: String,
        amount: ULong,
        activityResultSender: ActivityResultSender
    ): Flow<Result<String>> = flow {
        try {
            Log.d(TAG, "Unstaking USDC for account: $accountPublicKey, amount: $amount")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "Unstake USDC authResult.authToken: ${authResult.authToken}")

                val builder = Message.Builder()
                val instructions = genRequestUnstakeInstructions(accountPublicKey, amount)

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

                Log.d(TAG, "signAndSendTransactions (unstake): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (unstake): $signResult")
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(TAG, "Unstake USDC successful: $signatureString")
                        emit(Result.success(signatureString))
                    } else {
                        Log.e(TAG, "No signature returned from unstake transaction")
                        emit(Result.failure(Exception("No signature returned from transaction")))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for unstake")
                    emit(Result.failure(Exception("No wallet found")))
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Unstake failed: ${result.e.message}")
                    emit(Result.failure(Exception("Transaction failed: ${result.e.message}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unstake exception:", e)
            emit(Result.failure(Exception("Failed to unstake USDC: ${e.message}")))
        }
    }

    suspend fun unstake(
        accountPublicKey: String,
        amount: ULong,
        activityResultSender: ActivityResultSender
    ): Flow<Result<String>> = flow {
        try {
            Log.d(TAG, "Unstaking USDC for account: $accountPublicKey, amount: $amount")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "Unstake USDC authResult.authToken: ${authResult.authToken}")

                val builder = Message.Builder()
                val instructions = genUnstakeInstructions(accountPublicKey, amount)

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

                Log.d(TAG, "signAndSendTransactions (unstake): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (unstake): $signResult")
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(TAG, "Unstake USDC successful: $signatureString")
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(solanaRpcClient, signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d(TAG, "Transaction confirmed: $signatureString")
                            emit(Result.success(signatureString))
                        } else {
                            Log.e(TAG, "Transaction confirmation failed: $signatureString")
                            emit(Result.failure(Exception("Transaction confirmation failed")))
                        }
                    } else {
                        Log.e(TAG, "No signature returned from unstake transaction")
                        emit(Result.failure(Exception("No signature returned from transaction")))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for unstake")
                    emit(Result.failure(Exception("No wallet found")))
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Unstake failed: ${result.e.message}")
                    emit(Result.failure(Exception("Transaction failed: ${result.e.message}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unstake exception:", e)
            emit(Result.failure(Exception("Failed to unstake USDC: ${e.message}")))
        }
    }

    suspend fun initializeVaultDepositor(
        accountPublicKey: String,
        activityResultSender: ActivityResultSender
    ): Flow<Result<String>> = flow {
        try {
            Log.d(TAG, "Initializing vault depositor for account: $accountPublicKey")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(
                    TAG,
                    "Initialize vault depositor authResult.authToken: ${authResult.authToken}"
                )

                val builder = Message.Builder()
                val instructions = genInitializeVaultDepositorInstructions(accountPublicKey)

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

                Log.d(TAG, "signAndSendTransactions (initialize): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (initialize): $signResult")
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString = Base58.encodeToString(signature)
                        Log.d(TAG, "Initialize vault depositor successful: $signatureString")
                        
                        // Confirm transaction
                        val confirmationResult = Utils.confirmTransaction(solanaRpcClient, signatureString)
                        if (confirmationResult.isSuccess && confirmationResult.getOrNull() == true) {
                            Log.d(TAG, "Transaction confirmed: $signatureString")
                            emit(Result.success(signatureString))
                        } else {
                            Log.e(TAG, "Transaction confirmation failed: $signatureString")
                            emit(Result.failure(Exception("Transaction confirmation failed")))
                        }
                    } else {
                        Log.e(TAG, "No signature returned from initialize transaction")
                        emit(Result.failure(Exception("No signature returned from transaction")))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for initialize")
                    emit(Result.failure(Exception("No wallet found")))
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Initialize failed: ${result.e.message}")
                    emit(Result.failure(Exception("Transaction failed: ${result.e.message}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Initialize exception:", e)
            emit(Result.failure(Exception("Failed to initialize vault depositor: ${e.message}")))
        }
    }

    suspend fun getVaultInfoWithStakers(accountPublicKey: String): Flow<Result<VaultInfoWithStakers>> =
        flow {
            try {
                Log.d(TAG, "Getting vault info with stakers for account: $accountPublicKey")

                // Get vault info
                val vault = getVaultInfoFromChain(accountPublicKey)

                // Get total stakers count
                val totalStakers = VaultUtils.getTotalStakers(solanaRpcClient)

                // Combine into wrapper
                val vaultInfoWithStakers = VaultInfoWithStakers(
                    vault = vault,
                    totalStakers = totalStakers
                )

                Log.d(TAG, "Successfully retrieved vault info with ${totalStakers} stakers")
                emit(Result.success(vaultInfoWithStakers))
            } catch (e: Exception) {
                Log.e(TAG, "Error getting vault info with stakers: ${e.message}", e)
                emit(Result.failure(Exception("Failed to get vault info with stakers: ${e.message}")))
            }
        }

    private suspend fun getVaultInfoFromChain(accountPublicKey: String): Vault {
        val pda = VaultUtils.getVaultPda()
        Log.d(TAG, "vault pda : ${pda.base58()}")

        try {
            val vaultData = solanaRpcClient.getAccountInfo<Vault>(
                pda,
                dataSlice = AccountRequest.DataSlice(
                    297, 40
                )
            ).result?.data

            if (vaultData != null) {
                Log.d(TAG, "Vault data is : $vaultData")
                return vaultData
            } else {
                Log.w(TAG, "Vault data is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding vault data: ${e.message}", e)
        }

        // If parsing fails, return default data
        return Vault(
            pubkey = pda,
            owner = SolanaPublicKey.from("11111111111111111111111111111111"),
            platformAccount = SolanaPublicKey.from("11111111111111111111111111111111"),
            tokenMint = SolanaPublicKey.from("11111111111111111111111111111111"),
            vaultTokenAccount = SolanaPublicKey.from("11111111111111111111111111111111"),
            totalShares = 1234UL,
            totalAssets = 850000000UL, // 850000 USDC (6 decimals)
            totalRewards = 42500000UL, // 42.5 USDC (6 decimals)
            rewardsPerShare = 8500000UL, // 8.5% APY
            rewardsPerShare2 = 8500000UL, // 8.5% APY
            lastRewardsUpdate = System.currentTimeMillis(),
            unstakeLockupPeriod = 1209600L, // 14 days (14 * 24 * 60 * 60 = 1209600 seconds)
            managementFee = 5000UL, // 50% (5000 basis points)
            minStakeAmount = 1000000UL, // 1 USDC minimum
            maxTotalAssets = 1000000000000UL, // 1M USDC max
            isPaused = false,
            createdAt = System.currentTimeMillis(),
            sharesBase = 1U,
            rebaseVersion = 1U,
            ownerShares = 0UL,
            pendingUnstakeShares = 0UL,
            reservedAssets = 0UL,
            bump = 0U
        )
    }

    private suspend fun getStakingInfoFromChain(accountPublicKey: String): VaultDepositor? {
        val vaultDepositorPda =
            VaultUtils.getVaultDepositorPda(SolanaPublicKey.from(accountPublicKey))
        val vaultDepositor =
            solanaRpcClient.getAccountInfo<VaultDepositor>(
                vaultDepositorPda,
            ).result?.data
        Log.d(TAG, "Staking info is : $vaultDepositor")
        return vaultDepositor
    }

    private suspend fun getStakeActivitiesFromChain(accountPublicKey: String): List<StakeActivity> {
        // TODO: Implement actual blockchain call to get stake activities
        // For now, return mock data
        return listOf(
            StakeActivity("Stake", "$1000", "August 15, 2023", true),
            StakeActivity("Withdraw Stake", "$500", "May 10, 2023", false)
        )
    }

    private suspend fun genStakeInstructions(
        accountPublicKey: String,
        amount: ULong
    ): List<TransactionInstruction> {
        val userPublicKey = SolanaPublicKey.from(accountPublicKey)
        val vaultPda = VaultUtils.getVaultPda()
        val vaultDepositorPda = VaultUtils.getVaultDepositorPda(userPublicKey)
        val vaultTokenAccountPda = VaultUtils.getVaultTokenAccountPda()

        val instructions = ArrayList<TransactionInstruction>()

        val userTokenAccount = ShopUtils.getAssociatedTokenAddress(userPublicKey).getOrNull()!!

        val stakeInstruction = genTransactionInstruction(
            listOf(
                AccountMeta(vaultPda, false, true),
                AccountMeta(vaultDepositorPda, false, true),
                AccountMeta(vaultTokenAccountPda, false, true),
                AccountMeta(userTokenAccount, false, true),
                AccountMeta(userPublicKey, true, true),
                AccountMeta(
                    SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID),
                    false,
                    false
                )
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("stake"),
                amount
            ),
            AppConstants.App.getVaultProgramId()
        )
        instructions.add(stakeInstruction)
        return instructions
    }

    private suspend fun genRequestUnstakeInstructions(
        accountPublicKey: String,
        amount: ULong
    ): List<TransactionInstruction> {
        val userPublicKey = SolanaPublicKey.from(accountPublicKey)

        val vaultPda = VaultUtils.getVaultPda()
        val vaultDepositorPda = VaultUtils.getVaultDepositorPda(userPublicKey)

        val unstakeInstruction = genTransactionInstruction(
            listOf(
                AccountMeta(vaultPda, false, true),
                AccountMeta(vaultDepositorPda, false, true),
                AccountMeta(userPublicKey, true, true)
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("request_unstake"),
                amount
            ),
            AppConstants.App.getVaultProgramId()
        )

        return listOf(unstakeInstruction)
    }

    private suspend fun genUnstakeInstructions(
        accountPublicKey: String,
        amount: ULong
    ): List<TransactionInstruction> {
        val userPublicKey = SolanaPublicKey.from(accountPublicKey)
        val usdcMint = AppConstants.App.getMint()

        val vaultPda = VaultUtils.getVaultPda()
        val vaultDepositorPda = VaultUtils.getVaultDepositorPda(userPublicKey)
        val vaultTokenAccountPda = VaultUtils.getVaultTokenAccountPda()
        val userTokenAccount = ShopUtils.getAssociatedTokenAddress(userPublicKey).getOrNull()!!

        val unstakeInstruction = genTransactionInstruction(
            listOf(
                AccountMeta(vaultPda, false, true),
                AccountMeta(vaultDepositorPda, false, true),
                AccountMeta(vaultTokenAccountPda, false, true),
                AccountMeta(userTokenAccount, false, true),
                AccountMeta(userPublicKey, true, true),
                AccountMeta(
                    SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID),
                    false,
                    false
                )
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("unstake"),
                Unit
            ),
            AppConstants.App.getVaultProgramId()
        )

        return listOf(unstakeInstruction)
    }

    private suspend fun genInitializeVaultDepositorInstructions(
        accountPublicKey: String
    ): List<TransactionInstruction> {
        val userPublicKey = SolanaPublicKey.from(accountPublicKey)
        val vaultPda = VaultUtils.getVaultPda()
        val vaultDepositorPda = VaultUtils.getVaultDepositorPda(userPublicKey)

        val initializeInstruction = genTransactionInstruction(
            listOf(
                AccountMeta(vaultPda, false, false),
                AccountMeta(vaultDepositorPda, false, true),
                AccountMeta(userPublicKey, true, true),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false),
                AccountMeta(
                    SolanaPublicKey.from("SysvarRent111111111111111111111111111111111"),
                    false,
                    false
                ),
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("initialize_vault_depositor"),
                Unit
            ),
            AppConstants.App.getVaultProgramId()

        )

        return listOf(initializeInstruction)
    }
}

@Serializable
data class StakeRequest(
    val amount: ULong
)

@Serializable
data class UnstakeRequest(
    val amount: ULong
) 