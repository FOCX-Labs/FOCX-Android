package com.focx.data.datasource.solana

import android.content.Context
import com.focx.core.constants.AppConstants
import com.focx.core.network.NetworkConfig
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.focx.utils.Utils
import com.focx.utils.ShopUtils.genTransactionInstruction
import com.funkatronics.kborsh.Borsh
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.programs.SystemProgram
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
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

class SolanaFaucetDataSource @Inject constructor(
    private val context: Context,
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val solanaRpcClient: SolanaRpcClient
) {
    companion object {
        private const val TAG = "SFDS"
        private const val PROGRAM_ID = "3JtTpsLxSAYZweorwjU9cywAFLm8BUonGwQ54gqFnAGg"

        private suspend fun getFaucetPda(): SolanaPublicKey {
            return ProgramDerivedAddress.find(
                listOf(
                    "faucet".toByteArray()
                ),
                SolanaPublicKey.from(PROGRAM_ID)
            ).getOrNull()!!
        }
    }

    suspend fun requestUsdcFaucet(
        accountPublicKey: String,
        activityResultSender: ActivityResultSender,
        solAmount: Double
    ): Flow<Result<String>> = flow {
        try {
            Log.d(
                TAG,
                "Starting USDC faucet request for account: $accountPublicKey, SOL amount: $solAmount"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "USDC faucet authResult.authToken: ${authResult.authToken}")

                val builder = Message.Builder()
                val instructions = genUsdcFaucetInstructions(accountPublicKey, (solAmount * 1_000_000_000).toULong())

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

                Log.d(TAG, "signAndSendTransactions (USDC faucet): before")
                val signResult = signAndSendTransactions(
                    arrayOf(transaction.serialize())
                )
                Log.d(TAG, "signAndSendTransactions (USDC faucet): $signResult")
                signResult
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        val signatureString =
                            com.funkatronics.encoders.Base58.encodeToString(signature)
                        Log.d(TAG, "USDC faucet successful: $signatureString")
                        
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
                        Log.e(TAG, "No signature returned from USDC faucet transaction")
                        emit(Result.failure(Exception("No signature returned from transaction")))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for USDC faucet")
                    emit(Result.failure(Exception("No wallet found")))
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "USDC faucet failed: ${result.e.message}")
                    emit(Result.failure(Exception("Transaction failed: ${result.e.message}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "USDC faucet exception:", e)
            emit(Result.failure(Exception("Failed to request USDC faucet: ${e.message}")))
        }
    }

    private suspend fun genUsdcFaucetInstructions(
        accountPublicKey: String,
        usdcAmount: ULong
    ): List<TransactionInstruction> {
        val userPublicKey = SolanaPublicKey.from(accountPublicKey)
        val usdcMint = AppConstants.App.getMint()
        val faucetPda = getFaucetPda()
        val userTokenAccount = ShopUtils.getAssociatedTokenAddress(userPublicKey).getOrNull()!!
        val faucetTokenAccount = ShopUtils.getAssociatedTokenAddress(faucetPda).getOrNull()!!
        val faucetAuthority = SolanaPublicKey.from("DjBk7pZfKTnvHg1nhowR6HzTJpVijgoWzZTArm7Yra6X")

        val instructions = ArrayList<TransactionInstruction>()
        val userTokenData = solanaRpcClient.getAccountInfo(userTokenAccount).result?.data

        if (userTokenData == null) {
            val createTokenAccountInstruction = genTransactionInstruction(
                listOf(
                    AccountMeta(userPublicKey, isSigner = true, isWritable = true),
                    AccountMeta(userTokenAccount, isSigner = false, isWritable = true),
                    AccountMeta(userPublicKey, isSigner = true, isWritable = true),
                    AccountMeta(usdcMint, isSigner = false, isWritable = false),
                    AccountMeta(
                        SystemProgram.PROGRAM_ID,
                        false,
                        false
                    ), // system program,
                    AccountMeta(
                        SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID),
                        false,
                        false
                    )
                ),
                byteArrayOf(0),
                SolanaPublicKey.from(AppConstants.App.ASSOCIATED_TOKEN_PROGRAM_ID)
            )
            instructions.add(createTokenAccountInstruction)
        }


        // USDC faucet instruction - simplified version without ATA creation
        val faucetInstruction = genTransactionInstruction(
            listOf(
                AccountMeta(faucetPda, false, true),
                AccountMeta(userPublicKey, true, true),
                AccountMeta(usdcMint, false, false),
                AccountMeta(userTokenAccount, false, true),
                AccountMeta(faucetTokenAccount, false, true),
                AccountMeta(faucetAuthority, false, false),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false),
                AccountMeta(
                    SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID),
                    false,
                    false
                )
            ),
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("exchange_sol_for_tokens"),
                UsdcFaucetRequest(usdcAmount)
            ),
            SolanaPublicKey.from(PROGRAM_ID)
        )
        instructions.add(faucetInstruction)

        return instructions
    }
}

@Serializable
data class UsdcFaucetRequest(
    val amount: ULong
)

@Serializable
data class Faucet(
    val discriminator: Long,
    val authority: SolanaPublicKey,
    val tokenMint: SolanaPublicKey,
    val tokenAccount: SolanaPublicKey,
    val bump: UByte
)