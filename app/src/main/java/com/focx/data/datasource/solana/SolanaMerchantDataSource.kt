package com.focx.data.datasource.solana

import android.content.Context
import android.net.Uri
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
import com.focx.utils.Log
import com.focx.utils.ShopUtils.getDepositEscrowPda
import com.focx.utils.ShopUtils.getGlobalRootPda
import com.focx.utils.ShopUtils.getInitialChunkPda
import com.focx.utils.ShopUtils.getMerchantIdPda
import com.focx.utils.ShopUtils.getMerchantInfoPda
import com.focx.utils.ShopUtils.getSystemConfigPDA
import com.focx.utils.SolanaTokenUtils
import com.funkatronics.encoders.Base58
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.programs.SystemProgram
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.AccountInfo
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.getAccountInfo
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import com.syntifi.near.borshj.Borsh
import com.syntifi.near.borshj.annotation.BorshField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RegisterMerchantAtomicArgs(
    @BorshField(order = 1) val name: String, @BorshField(order = 2) val description: String
) : Borsh

@Serializable
private data class Args_increment(
    @BorshField(order = 1) val amount: UInt
) : Borsh

@Singleton
class SolanaMerchantDataSource @Inject constructor(
    private val context: Context,
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val solanaRpcClient: SolanaRpcClient
) : IMerchantRepository {

    companion object {
        private const val TAG = "SMDS"

        /**
         * Create instruction discriminator from method name using Anchor's algorithm
         */
        private fun createInstructionDiscriminator(instructionName: String): ByteArray {
            val hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest("global:$instructionName".toByteArray())
            return hash.take(8).toByteArray()
        }
    }

    suspend fun registerMerchantAtomic1(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(
                TAG,
                "Starting registerMerchantAtomic walletAdapter authToken: ${walletAdapter.authToken}"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "registerMerchantAtomic authResult.authToken:${authResult.authToken}")
                val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)
                val programId = SolanaPublicKey.from(merchantRegistration.programId)

                val globalRootPda = SolanaTokenUtils.findProgramAddress(
                    listOf("global_id_root".toByteArray()), programId.bytes
                )

                val merchantInfoPda = SolanaTokenUtils.findProgramAddress(
                    listOf(
                        "merchant_info".toByteArray(), merchantPublicKey.bytes
                    ), programId.bytes
                )
                val systemConfigPda = SolanaTokenUtils.findProgramAddress(
                    listOf("system_config".toByteArray()), programId.bytes
                )
                val merchantIdAccountPda = SolanaTokenUtils.findProgramAddress(
                    listOf("merchant".toByteArray(), merchantPublicKey.bytes), programId.bytes
                )

                val initialChunkPda = SolanaTokenUtils.findProgramAddress(
                    listOf(
                        "id_chunk".toByteArray(),
                        merchantPublicKey.bytes,
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()
                    ), programId.bytes
                )

                Log.d(TAG, "Calculated PDAs:")
                Log.d(TAG, "  globalRootPda: ${globalRootPda.first.base58()}")
                Log.d(TAG, "  merchantInfoPda: ${merchantInfoPda.first.base58()}")
                Log.d(TAG, "  merchantIdAccountPda: ${merchantIdAccountPda.first.base58()}")
                Log.d(TAG, "  initialChunkPda: ${initialChunkPda.first.base58()}")

                val instructionData = createRegisterMerchantAtomicInstructionData(
                    merchantRegistration.name, merchantRegistration.description
                )

                val accountMetas = listOf(
                    AccountMeta(merchantPublicKey, true, true),
                    AccountMeta(merchantPublicKey, true, true),
                    AccountMeta(globalRootPda.first, false, true),
                    AccountMeta(systemConfigPda.first, false, true),
                    AccountMeta(merchantInfoPda.first, false, true),
                    AccountMeta(
                        merchantIdAccountPda.first, false, true
                    ),
                    AccountMeta(initialChunkPda.first, false, true),
                    AccountMeta(SolanaTokenUtils.ProgramIds.SYSTEM_PROGRAM, false, false)
                )

                val instruction = genTransactionInstruction(
                    programId, accountMetas, instructionData
                )

                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                val message = Message.Builder().addInstruction(instruction)
                    .setRecentBlockhash(recentBlockhash).build()
                val transaction = Transaction(message)

                Log.d(TAG, "  signAndSendTransactions: before}")
                val result = signAndSendTransactions(arrayOf(transaction.serialize()))
                Log.d(TAG, "  signAndSendTransactions:${result}")
                result
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(
                            TAG, "Registration successful: ${Base58.encodeToString(signature)}"
                        )
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = merchantRegistration.merchantPublicKey,
                            errorMessage = null
                        )
                    } else {
                        MerchantRegistrationResult(
                            success = false,
                            transactionSignature = null,
                            merchantAccount = null,
                            errorMessage = "No signature returned from transaction"
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "No wallet found"
                    )
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Registration failed: ${result.e.message}")
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "Transaction failed: ${result.e.message}"
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Registration exception:", e)
            MerchantRegistrationResult(
                success = false,
                transactionSignature = null,
                merchantAccount = null,
                errorMessage = "Registration failed: ${e.message}"
            )
        }
    }

    /**
     * 使用 Anchor 和 Borsh 序列化方式注册商户。
     * 此方法遵循 Anchor 合约接口定义 (`solana_e_commerce.json`)。
     */
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
                    genDepositMerchantDepositInstruction(merchantRegistration, activityResultSender)

                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }
                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder().addInstruction(registerInstruction)
                    .addInstruction(depositInstruction).setRecentBlockhash(recentBlockhash).build()
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
                        Log.d(
                            TAG,
                            "Anchor registration successful: ${Base58.encodeToString(signature)}"
                        )
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = merchantRegistration.merchantPublicKey
                        )
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

        val programId = SolanaPublicKey.from(AppConstants.App.PROGRAM_ID)
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)


        val globalRootPda = getGlobalRootPda(programId)
        val merchantInfoPda = getMerchantInfoPda(merchantPublicKey, programId)
        val systemConfigPda = getSystemConfigPDA(programId)
        val merchantIdAccountPda = getMerchantIdPda(merchantPublicKey, programId)
        val initialChunkPda = getInitialChunkPda(merchantPublicKey, programId)

        val args = RegisterMerchantAtomicArgs(
            name = merchantRegistration.name, description = merchantRegistration.description
        )

        val argsBytes = Borsh.serialize(args)
        val discriminator = createInstructionDiscriminator("register_merchant_atomic")
        val instructionData = discriminator + argsBytes

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


        return genTransactionInstruction(programId, accountMetas, instructionData)
    }

    private suspend fun genDepositMerchantDepositInstruction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): TransactionInstruction {
        Log.d(TAG, "genDepositMerchantDepositTransaction start")

        val programId = SolanaPublicKey.from(AppConstants.App.PROGRAM_ID)
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)


        RegisterMerchantAtomicArgs(
            name = merchantRegistration.name, description = merchantRegistration.description
        )


        val argsBytes = Borsh.serialize(merchantRegistration.securityDeposit)
        val discriminator = createInstructionDiscriminator("deposit_merchant_deposit")
        val instructionData = discriminator + argsBytes


        val merchantInfoPda = getMerchantInfoPda(merchantPublicKey, programId)
        val systemConfigPda = getSystemConfigPDA(programId)
        val depositEscrowPda = getDepositEscrowPda(programId)

        val merchantTokenAccount = SolanaTokenUtils.getAssociatedTokenAddress(
            SolanaPublicKey.from(AppConstants.App.USDC_FOCX_MINT), merchantPublicKey
        )

        val accountMetas = listOf(
            AccountMeta(merchantPublicKey, true, true),
            AccountMeta(merchantInfoPda.getOrNull()!!, false, true),
            AccountMeta(systemConfigPda.getOrNull()!!, false, false),
            AccountMeta(merchantTokenAccount, false, true),
            AccountMeta(SolanaPublicKey.from(AppConstants.App.USDC_FOCX_MINT), false, false),
            AccountMeta(depositEscrowPda.getOrNull()!!, false, true),
            AccountMeta(SolanaPublicKey.from(SPL_TOKEN_PROGRAM_ID), false, false),
            AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )


        return genTransactionInstruction(programId, accountMetas, instructionData)
    }


    private suspend fun initializeDepositorTransaction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): Transaction {
        Log.d(TAG, "initializeDepositorTransaction start")
        val programId = SolanaPublicKey.from("EHiKn3J5wywNG2rHV2Qt74AfNqtJajhPerkVzYXudEwn")
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)


        val discriminator = createInstructionDiscriminator("initialize_vault_depositor")
        val instructionData = discriminator


        /**
         * {
         *           vault: vaultPDA,
         *           vaultDepositor: vaultDepositorPDA,
         *           authority: this.userWallet.publicKey,
         *           systemProgram: SystemProgram.programId,
         * }
         */
        val vaultPDA = ProgramDerivedAddress.find(
            listOf(
                "vault".toByteArray(), merchantPublicKey.bytes
            ), programId
        )
        val vaultDepositor = ProgramDerivedAddress.find(
            listOf(
                "vault_depositor".toByteArray(),
                vaultPDA.getOrNull()!!.bytes,
                merchantPublicKey.bytes
            ), programId
        )

        val accountMetas = listOf(
            AccountMeta(vaultPDA.getOrNull()!!, false, false),
            AccountMeta(vaultDepositor.getOrNull()!!, false, true),
            AccountMeta(merchantPublicKey, true, true),
            AccountMeta(SystemProgram.PROGRAM_ID, false, false),
            AccountMeta(
                SolanaPublicKey.from("SysvarRent111111111111111111111111111111111"), false, false
            )
        )
        Log.d(TAG, "programId: ${programId.base58()}")
        accountMetas.forEachIndexed { index, meta ->
            Log.d(
                TAG,
                "accountMetas[$index]: pubkey=${meta.publicKey.base58()}, isSigner=${meta.isSigner}, isWritable=${meta.isWritable}"
            )
        }
        Log.d(
            TAG, "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}"
        )


        val instruction = genTransactionInstruction(programId, accountMetas, instructionData)


        val recentBlockhash = try {
            kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                recentBlockhashUseCase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
            throw Exception("Failed to get recent blockhash: ${e.message}", e)
        }
        Log.d(TAG, "recentBlockhash: $recentBlockhash")
        val message =
            Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash)
                .build()
        val transaction = Transaction(message)
        return transaction
    }

    private suspend fun simpleInitialTransaction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): Transaction {
        val programId = SolanaPublicKey.from("96TkDXeRq7xGjmP1bzWn1kAVxukzpL1MsyajKX15fXyg")
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)

        val keyGen = java.security.KeyPairGenerator.getInstance(
            "Ed25519", org.bouncycastle.jce.provider.BouncyCastleProvider()
        )
        val keyPair = keyGen.generateKeyPair()
        val publicKeyBytes = keyPair.public.encoded.takeLast(32).toByteArray()
        val newAccount = SolanaPublicKey(publicKeyBytes)


        val argsBytes = Borsh.serialize(42L)
        val discriminator = createInstructionDiscriminator("initialize")
        val instructionData = discriminator + argsBytes

        Log.d(TAG, "Instruction data details:")
        Log.d(TAG, "  argsBytes: ${argsBytes.contentToString()}")
        Log.d(TAG, "  argsBytes hex: ${argsBytes.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  discriminator: ${discriminator.contentToString()}")
        Log.d(TAG, "  discriminator hex: ${discriminator.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  instructionData: ${instructionData.contentToString()}")
        Log.d(
            TAG, "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}"
        )
        Log.d(TAG, "  instructionData length: ${instructionData.size}")


        val accountMetas = listOf(

            AccountMeta(merchantPublicKey, true, true),
            AccountMeta(newAccount, true, true),
            AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )
        Log.d(TAG, "programId: ${programId.base58()}")
        accountMetas.forEachIndexed { index, meta ->
            Log.d(
                TAG,
                "accountMetas[$index]: pubkey=${meta.publicKey.base58()}, isSigner=${meta.isSigner}, isWritable=${meta.isWritable}"
            )
        }

        val instruction = genTransactionInstruction(programId, accountMetas, instructionData)


        val recentBlockhash = try {
            kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                recentBlockhashUseCase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
            throw Exception("Failed to get recent blockhash: ${e.message}", e)
        }
        Log.d(TAG, "recentBlockhash: $recentBlockhash")
        val message =
            Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash)
                .build()
        val transaction = Transaction(message)
        return transaction
    }

    private fun genTransactionInstruction(
        programId: SolanaPublicKey, accounts: List<AccountMeta>, data: ByteArray
    ): TransactionInstruction {
        Log.d(TAG, "============genTransactionInstruction : $programId")
        accounts.forEachIndexed { index, meta ->
            Log.d(
                TAG,
                "accountMetas[$index]: pubkey=${meta.publicKey.base58()}, isSigner=${meta.isSigner}, isWritable=${meta.isWritable}"
            )
        }
        Log.d(TAG, "  instructionData: ${data.contentToString()}")
        Log.d(TAG, "  instructionData hex: ${data.joinToString("") { "%02x".format(it) }}")
        return TransactionInstruction(programId, accounts, data)
    }

    private suspend fun simpleTestTransaction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): Transaction {
        val programId = SolanaPublicKey.from("96TkDXeRq7xGjmP1bzWn1kAVxukzpL1MsyajKX15fXyg")
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)

        val keyGen = java.security.KeyPairGenerator.getInstance(
            "Ed25519", org.bouncycastle.jce.provider.BouncyCastleProvider()
        )
        val keyPair = keyGen.generateKeyPair()
        val publicKeyBytes = keyPair.public.encoded.takeLast(32).toByteArray()
        SolanaPublicKey(publicKeyBytes)


        val argsBytes = Borsh.serialize(42L)
        val discriminator = createInstructionDiscriminator("test")
        val instructionData = discriminator + argsBytes

        Log.d(TAG, "Instruction data details:")
        Log.d(TAG, "  argsBytes: ${argsBytes.contentToString()}")
        Log.d(TAG, "  argsBytes hex: ${argsBytes.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  discriminator: ${discriminator.contentToString()}")
        Log.d(TAG, "  discriminator hex: ${discriminator.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  instructionData: ${instructionData.contentToString()}")
        Log.d(
            TAG, "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}"
        )
        Log.d(TAG, "  instructionData length: ${instructionData.size}")


        val accountMetas = listOf(


            AccountMeta(merchantPublicKey, true, true),
            AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )

        val instruction = genTransactionInstruction(programId, accountMetas, instructionData)


        val recentBlockhash = try {
            kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                recentBlockhashUseCase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
            throw Exception("Failed to get recent blockhash: ${e.message}", e)
        }
        Log.d(TAG, "recentBlockhash: $recentBlockhash")
        val message =
            Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash)
                .build()
        val transaction = Transaction(message)
        return transaction
    }

    /**
     * mock Solana Memo Program
     */
    private fun mockMemoTransaction(
        merchantPublicKey: SolanaPublicKey, recentBlockHash: SolanaPublicKey
    ): Transaction {
        val memoProgramId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val memoInstruction = genTransactionInstruction(
            memoProgramId,
            listOf(AccountMeta(merchantPublicKey, true, true)),
            "HelloFocxMemo".encodeToByteArray()
        )


        val memoTxMessage =
            Message.Builder().addInstruction(memoInstruction).setRecentBlockhash(recentBlockHash)
                .build()
        return Transaction(memoTxMessage)
    }

    suspend fun registerMerchantAtomic5(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(
                TAG, "Starting transactionDemo, walletAdapter authToken: ${walletAdapter.authToken}"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "transactionDemo authResult.authToken:${authResult.authToken}")

                val programId = SolanaPublicKey.from("ADraQ2ENAbVoVZhvH5SPxWPsF2hH5YmFcgx61TafHuwu")
                val seeds = listOf("counter".encodeToByteArray())
                val pdaResult = ProgramDerivedAddress.find(seeds, programId)
                val counterAccountPDA = pdaResult.getOrNull()

                Log.d(TAG, "Calculated PDA:")
                Log.d(TAG, "  counterAccountPDA: ${counterAccountPDA?.base58()}")


                val args = Args_increment(1u)


                val argsBytes = Borsh.serialize(args)
                val discriminator = createInstructionDiscriminator("increment")
                val instructionData = discriminator + argsBytes

                Log.d(TAG, "Instruction data details:")
                Log.d(TAG, "  args: amount=${args.amount}")
                Log.d(TAG, "  argsBytes: ${argsBytes.contentToString()}")
                Log.d(TAG, "  argsBytes hex: ${argsBytes.joinToString("") { "%02x".format(it) }}")
                Log.d(TAG, "  discriminator: ${discriminator.contentToString()}")
                Log.d(
                    TAG,
                    "  discriminator hex: ${discriminator.joinToString("") { "%02x".format(it) }}"
                )
                Log.d(TAG, "  instructionData: ${instructionData.contentToString()}")
                Log.d(
                    TAG,
                    "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}"
                )
                Log.d(TAG, "  instructionData length: ${instructionData.size}")


                val accountMetas = listOf(
                    AccountMeta(counterAccountPDA!!, false, true),
                    AccountMeta(SystemProgram.PROGRAM_ID, false, false)
                )


                val instruction =
                    genTransactionInstruction(programId, accountMetas, instructionData)


                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder().addInstruction(instruction)
                    .setRecentBlockhash(recentBlockhash).build()
                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (Demo): before")
                val signResult = signAndSendTransactions(arrayOf(transaction.serialize()))
                Log.d(TAG, "signAndSendTransactions (Demo): $signResult")
                signResult
            }


            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(
                            TAG, "Demo transaction successful: ${Base58.encodeToString(signature)}"
                        )
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = null,
                            errorMessage = null
                        )
                    } else {
                        MerchantRegistrationResult(
                            success = false,
                            transactionSignature = null,
                            merchantAccount = null,
                            errorMessage = "No signature returned from demo transaction"
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "No wallet found for demo"
                    )
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Demo transaction failed: ${result.e}")
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "Demo transaction failed: ${result.e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Demo transaction exception:", e)
            MerchantRegistrationResult(
                success = false,
                transactionSignature = null,
                merchantAccount = null,
                errorMessage = "Demo transaction failed: ${e.message}"
            )
        }
    }

    suspend fun registerMerchantAtomic4(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(
                TAG, "Starting transactionDemo, walletAdapter authToken: ${walletAdapter.authToken}"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "transactionDemo authResult.authToken:${authResult.authToken}")

                val memoProgramId = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
                val memoProgramIdKey = SolanaPublicKey.from(memoProgramId)


                val memoInstruction = genTransactionInstruction(
                    memoProgramIdKey,

                    listOf(
                        AccountMeta(
                            SolanaPublicKey.from(merchantRegistration.merchantPublicKey), true, true
                        )
                    ),

                    "Hello Solana!".encodeToByteArray()
                )


                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder().addInstruction(memoInstruction)
                    .setRecentBlockhash(recentBlockhash).build()
                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (Demo): before")
                val signResult = signAndSendTransactions(arrayOf(transaction.serialize()))
                Log.d(TAG, "signAndSendTransactions (Demo): $signResult")
                signResult
            }


            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(
                            TAG, "Demo transaction successful: ${Base58.encodeToString(signature)}"
                        )
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = null,
                            errorMessage = null
                        )
                    } else {
                        MerchantRegistrationResult(
                            success = false,
                            transactionSignature = null,
                            merchantAccount = null,
                            errorMessage = "No signature returned from demo transaction"
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "No wallet found for demo"
                    )
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "Demo transaction failed: ${result.e}")
                    MerchantRegistrationResult(
                        success = false,
                        transactionSignature = null,
                        merchantAccount = null,
                        errorMessage = "Demo transaction failed: ${result.e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Demo transaction exception:", e)
            MerchantRegistrationResult(
                success = false,
                transactionSignature = null,
                merchantAccount = null,
                errorMessage = "Demo transaction failed: ${e.message}"
            )
        }
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
                val pda = getMerchantInfoPda(
                    SolanaPublicKey.from(walletAddress),
                    SolanaPublicKey.from(AppConstants.App.PROGRAM_ID)
                )
                Log.d(TAG, "merchant pad: ${pda.getOrNull()!!.base58()}")
                val merchant = solanaRpcClient.getAccountInfo<Merchant>(pda.getOrNull()!!).result

                if (!(merchant == null || merchant.data == null)) {

                    val merchantStatus = MerchantStatus(
                        isRegistered = true,
                        merchantAccount = "aaa",
                        registrationDate = merchant.data!!.createdAt.toString(),
                        securityDeposit = merchant.data!!.depositAmount.toLong(),
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


    /**
     * Query account info from Solana RPC using SolanaRpcClient
     */
    private suspend fun queryAccountInfo(
        rpcUri: Uri, accountAddress: String
    ): AccountInfo<ByteArray>? {
        return try {
            val publicKey = SolanaPublicKey.from(accountAddress)
            val rpcResponse = solanaRpcClient.getAccountInfo(publicKey)
            rpcResponse.result
        } catch (e: Exception) {
            Log.e(TAG, "Error querying account info for $accountAddress: ${e.message}", e)
            null
        }
    }

    override suspend fun depositMerchantFunds(
        merchantAccount: String, depositAmount: Long, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(
                TAG,
                "Starting depositMerchantFunds for merchant: $merchantAccount, amount: $depositAmount"
            )

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val merchantPublicKey = SolanaPublicKey.from(merchantAccount)
                val authorityPublicKey = SolanaPublicKey.from(AppConstants.App.AUTHORITY_PUBLIC_KEY)
                val programId = SolanaPublicKey.from(AppConstants.App.PROGRAM_ID)


                val merchantInfoPda = SolanaTokenUtils.findProgramAddress(
                    listOf("merchant_info".toByteArray(), merchantPublicKey.bytes), programId.bytes
                )

                val systemConfigPda = SolanaTokenUtils.findProgramAddress(
                    listOf("system_config".toByteArray()), programId.bytes
                )


                val usdcMint = SolanaTokenUtils.TokenMints.USDC
                val tokenProgramId = SolanaTokenUtils.ProgramIds.TOKEN_PROGRAM


                val merchantUsdcAccount =
                    SolanaTokenUtils.getUsdcAssociatedTokenAccount(merchantPublicKey)

                val programUsdcAccount = SolanaTokenUtils.getUsdcAssociatedTokenAccount(programId)

                Log.d(TAG, "Calculated PDAs for deposit:")
                Log.d(TAG, "  merchantInfoPda: ${merchantInfoPda.first.base58()}")
                Log.d(TAG, "  systemConfigPda: ${systemConfigPda.first.base58()}")
                Log.d(TAG, "  merchantUsdcAccount: ${merchantUsdcAccount.base58()}")
                Log.d(TAG, "  programUsdcAccount: ${programUsdcAccount.base58()}")
                Log.d(TAG, "  usdcMint: ${usdcMint.base58()}")

                val instructionData = createDepositMerchantFundsInstructionData(depositAmount)

                val accountMetas = listOf(
                    AccountMeta(merchantPublicKey, true, true),
                    AccountMeta(authorityPublicKey, true, false),
                    AccountMeta(merchantInfoPda.first, false, true),
                    AccountMeta(systemConfigPda.first, false, true),
                    AccountMeta(merchantUsdcAccount, false, true),
                    AccountMeta(programUsdcAccount, false, true),
                    AccountMeta(tokenProgramId, false, false)
                )

                val instruction = genTransactionInstruction(
                    programId, accountMetas, instructionData
                )

                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash for deposit: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                val message = Message.Builder().addInstruction(instruction)
                    .setRecentBlockhash(recentBlockhash).build()
                val transaction = Transaction(message)

                Log.d(TAG, "Executing depositMerchantFunds transaction")
                val result = signAndSendTransactions(arrayOf(transaction.serialize()))
                Log.d(TAG, "depositMerchantFunds result: $result")
                result
            }

            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(TAG, "Deposit successful: ${Base58.encodeToString(signature)}")
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = merchantAccount,
                            errorMessage = null
                        )
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

    /**
     * Create instruction data for depositMerchantFunds method
     * Based on the Anchor program method signature
     */
    private fun createDepositMerchantFundsInstructionData(depositAmount: Long): ByteArray {
        val discriminator = byteArrayOf(
            0x2E.toByte(),
            0x8A.toByte(),
            0x1F.toByte(),
            0x3C.toByte(),
            0x9B.toByte(),
            0x7D.toByte(),
            0x4E.toByte(),
            0x6F.toByte()
        )

        val amountBytes =
            ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(depositAmount).array()
        return discriminator + amountBytes
    }

    /**
     * Create instruction data for registerMerchantAtomic method
     * Based on the Anchor program method signature
     */
    private fun createRegisterMerchantAtomicInstructionData(
        merchantName: String, merchantDescription: String
    ): ByteArray {
        val discriminator = byteArrayOf(
            0x8C.toByte(),
            0x97.toByte(),
            0x25.toByte(),
            0x8F.toByte(),
            0x4C.toByte(),
            0x2A.toByte(),
            0x9D.toByte(),
            0x12.toByte()
        )

        val nameBytes = merchantName.toByteArray(Charsets.UTF_8)
        val nameLength =
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(nameBytes.size).array()

        val descriptionBytes = merchantDescription.toByteArray(Charsets.UTF_8)
        val descriptionLength =
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(descriptionBytes.size)
                .array()

        return discriminator + nameLength + nameBytes + descriptionLength + descriptionBytes
    }
}