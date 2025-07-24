package com.focx.data.datasource.solana

import android.content.Context
import android.net.Uri
import com.focx.core.constants.AppConstants
import com.focx.core.network.NetworkConfig
import com.focx.domain.entity.MerchantRegistration
import com.focx.domain.entity.MerchantRegistrationResult
import com.focx.domain.entity.MerchantStatus
import com.focx.domain.repository.IMerchantRepository
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.Log
import com.focx.utils.SolanaTokenUtils
import com.funkatronics.encoders.Base58
import com.syntifi.near.borshj.Borsh
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.programs.SystemProgram
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.AccountInfo
import com.solana.rpc.SolanaRpcClient
import com.solana.serialization.AnchorInstructionSerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import com.syntifi.near.borshj.annotation.BorshField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

// TODO anchor: 这是根据 solana_e_commerce.json 为 register_merchant_atomic 指令定义的参数结构。
@Serializable
private data class RegisterMerchantAtomicArgs(
    @BorshField(order = 1)
    val name: String,
    @BorshField(order = 2)
    val description: String
) : com.syntifi.near.borshj.Borsh


@Serializable
private data class Args_increment(
    @BorshField(order = 1)
    val amount: UInt
): com.syntifi.near.borshj.Borsh

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
            Log.d(TAG, "Starting registerMerchantAtomic walletAdapter authToken: ${walletAdapter.authToken}")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "registerMerchantAtomic authResult.authToken:${authResult.authToken}")
                val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)
                val programId = SolanaPublicKey.from(merchantRegistration.programId)

                // Calculate PDAs (Program Derived Addresses)
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

                // Create instruction data for registerMerchantAtomic
                val instructionData = createRegisterMerchantAtomicInstructionData(
                    merchantRegistration.name, merchantRegistration.description
                )

                // Create account metas for the instruction
                val accountMetas = listOf(
                    AccountMeta(merchantPublicKey, true, true), // merchant (signer, writable)
                    AccountMeta(merchantPublicKey, true, true), // payer (signer, writable)
                    AccountMeta(globalRootPda.first, false, true), // globalRoot (writable)
                    AccountMeta(systemConfigPda.first, false, true),
                    AccountMeta(merchantInfoPda.first, false, true), // merchantInfo (writable)
                    AccountMeta(merchantIdAccountPda.first, false, true), // merchantIdAccount (writable)
                    AccountMeta(initialChunkPda.first, false, true), // initialChunk (writable)
                    AccountMeta(SolanaTokenUtils.ProgramIds.SYSTEM_PROGRAM, false, false)
                )

                // Create the instruction
                val instruction = genTransactionInstruction(
                    programId, accountMetas, instructionData
                )

                // Create and sign the transaction
                // Get the actual recent blockhash from Solana network with timeout handling
                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                val message = Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash).build()
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
            Log.d(TAG, "Starting registerMerchantWithAnchor, walletAdapter authToken: ${walletAdapter.authToken}")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "registerMerchantWithAnchor authResult.authToken:${authResult.authToken}")

//                val transaction = simpleTestTransaction(merchantRegistration, activityResultSender)
                val transaction = genRegisterTransaction(merchantRegistration, activityResultSender)

                Log.d(TAG, "signAndSendTransactions (Anchor): before")
                val signResult = signAndSendTransactions(arrayOf(transaction.serialize()))
                Log.d(TAG, "signAndSendTransactions (Anchor): $signResult")
                signResult
            }

            // 处理交易结果 (与 registerMerchantAtomic 相同)
            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(TAG, "Anchor registration successful: ${Base58.encodeToString(signature)}")
                        MerchantRegistrationResult(
                            success = true,
                            transactionSignature = Base58.encodeToString(signature),
                            merchantAccount = merchantRegistration.merchantPublicKey
                        )
                    } else {
                        MerchantRegistrationResult(
                            success = false,
                            errorMessage = "No signature returned from transaction"
                        )
                    }
                }
                is TransactionResult.NoWalletFound -> {
                    MerchantRegistrationResult(
                        success = false,
                        errorMessage = "No wallet found"
                    )
                }
                is TransactionResult.Failure -> {
                    Log.e(TAG, "Anchor registration failed: ${result.e}")
                    MerchantRegistrationResult(
                        success = false,
                        errorMessage = "Transaction failed: ${result.e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Anchor registration exception:", e)
            MerchantRegistrationResult(
                success = false,
                errorMessage = "Registration failed: ${e.message}"
            )
        }
    }

    private suspend fun genRegisterTransaction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): Transaction {
        Log.d(TAG, "genRegisterTransaction start")
        // 合约地址来自 solana_e_commerce.json
        val programId = SolanaPublicKey.from("mo5xPstZDm27CAkcyoTJnEovMYcW45tViAU6PZikv5q")
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)

        // 计算 PDAs (与 registerMerchantAtomic 相同)
        val globalRootPda = ProgramDerivedAddress.find(
            listOf("global_id_root".toByteArray()), programId
        )
        val merchantInfoPda = ProgramDerivedAddress.find(
            listOf("merchant_info".toByteArray(), merchantPublicKey.bytes), programId
        )
        val systemConfigPda = ProgramDerivedAddress.find(
            listOf("system_config".toByteArray()), programId
        )
        val merchantIdAccountPda = ProgramDerivedAddress.find(
            listOf("merchant_id".toByteArray(), merchantPublicKey.bytes), programId
        )
        val initialChunkPda = ProgramDerivedAddress.find(
            listOf(
                "id_chunk".toByteArray(),
                merchantPublicKey.bytes,
                byteArrayOf(0),
            ), programId
        )

        Log.d(TAG, "Calculated PDAs for Anchor:")
        Log.d(TAG, "  globalRootPda: ${globalRootPda.getOrNull()!!.base58()}")
        Log.d(TAG, "  systemConfigPda: ${systemConfigPda.getOrNull()!!.base58()}")
        Log.d(TAG, "  merchantInfoPda: ${merchantInfoPda.getOrNull()!!.base58()}")
        Log.d(TAG, "  merchantIdAccountPda: ${merchantIdAccountPda.getOrNull()!!.base58()}")
        Log.d(TAG, "  initialChunkPda: ${initialChunkPda.getOrNull()!!.base58()}")

        // 1. 构造 Anchor 指令的参数
        val args = RegisterMerchantAtomicArgs(
            name = merchantRegistration.name,
            description = merchantRegistration.description
        )

        // 2. 使用 AnchorInstructionSerializer 和 Borsh 对指令数据进行序列化
        //    指令名称 "register_merchant_atomic" 必须与 Anchor 合约中的方法名完全匹配。
        // 使用 borshj 进行序列化
        val argsBytes = Borsh.serialize(args)
        val discriminator = createInstructionDiscriminator("register_merchant_atomic")
        val instructionData = discriminator + argsBytes

        Log.d(TAG, "Instruction data details:")
        Log.d(TAG, "  args: name='${args.name}', description='${args.description}'")
        Log.d(TAG, "  argsBytes: ${argsBytes.contentToString()}")
        Log.d(TAG, "  argsBytes hex: ${argsBytes.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  discriminator: ${discriminator.contentToString()}")
        Log.d(TAG, "  discriminator hex: ${discriminator.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  instructionData: ${instructionData.contentToString()}")
        Log.d(TAG, "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  instructionData length: ${instructionData.size}")

        // 3. 创建账户列表 (AccountMeta)，顺序和属性必须与 solana_e_commerce.json 中的定义一致
        val accountMetas = listOf(
            AccountMeta(merchantPublicKey, true, true),                // merchant (signer, writable)
            AccountMeta(merchantPublicKey, true, true),                // payer (signer, writable)
            AccountMeta(globalRootPda.getOrNull()!!, false, true),             // global_root (writable)
            AccountMeta(merchantInfoPda.getOrNull()!!, false, true),           // merchant_info (writable)
            AccountMeta(systemConfigPda.getOrNull()!!, false, false),          // system_config (readonly)
            AccountMeta(merchantIdAccountPda.getOrNull()!!, false, true),      // merchant_id_account (writable)
            AccountMeta(initialChunkPda.getOrNull()!!, false, true),           // initial_chunk (writable)
            AccountMeta(SystemProgram.PROGRAM_ID, false, false) // system_program
        )

        // 4. 创建指令
        val instruction = genTransactionInstruction(programId, accountMetas, instructionData)

        // 5. 获取最新区块哈希并构建交易
        val recentBlockhash = try {
            kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                recentBlockhashUseCase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
            throw Exception("Failed to get recent blockhash: ${e.message}", e)
        }
        Log.d(TAG, "recentBlockhash: $recentBlockhash")
        val message = Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash).build()
        return Transaction(message)
    }


    private suspend fun initializeDepositorTransaction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ):Transaction {
        Log.d(TAG, "initializeDepositorTransaction start")
        val programId = SolanaPublicKey.from("EHiKn3J5wywNG2rHV2Qt74AfNqtJajhPerkVzYXudEwn")
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)

        // 2. 使用 AnchorInstructionSerializer 和 Borsh 对指令数据进行序列化
        // 使用 borshj 进行序列化
        val discriminator = createInstructionDiscriminator("initialize_vault_depositor")
        val instructionData = discriminator

        // 3. 创建账户列表 (AccountMeta)
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
                "vault".toByteArray(),
                merchantPublicKey.bytes
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
            AccountMeta(SolanaPublicKey.from("SysvarRent111111111111111111111111111111111"), false, false)
        )
        Log.d(TAG, "programId: ${programId.base58()}")
        accountMetas.forEachIndexed { index, meta ->
            Log.d(TAG, "accountMetas[$index]: pubkey=${meta.publicKey.base58()}, isSigner=${meta.isSigner}, isWritable=${meta.isWritable}")
        }
        Log.d(TAG, "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}")

        // 4. 创建指令
        val instruction = genTransactionInstruction(programId, accountMetas, instructionData)

        // 5. 获取最新区块哈希并构建交易
        val recentBlockhash = try {
            kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                recentBlockhashUseCase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
            throw Exception("Failed to get recent blockhash: ${e.message}", e)
        }
        Log.d(TAG, "recentBlockhash: $recentBlockhash")
        val message = Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash).build()
        val transaction = Transaction(message)
        return transaction
    }

    private suspend fun simpleInitialTransaction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ):Transaction {
        val programId = SolanaPublicKey.from("96TkDXeRq7xGjmP1bzWn1kAVxukzpL1MsyajKX15fXyg")
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)
        // 随机生成一个 Ed25519 密钥对，并取公钥
        val keyGen = java.security.KeyPairGenerator.getInstance("Ed25519", org.bouncycastle.jce.provider.BouncyCastleProvider())
        val keyPair = keyGen.generateKeyPair()
        val publicKeyBytes = keyPair.public.encoded.takeLast(32).toByteArray()
        val newAccount = SolanaPublicKey(publicKeyBytes)

        // 2. 使用 AnchorInstructionSerializer 和 Borsh 对指令数据进行序列化
        //    指令名称 "register_merchant_atomic" 必须与 Anchor 合约中的方法名完全匹配。
        // 使用 borshj 进行序列化
        val argsBytes = Borsh.serialize(42L)
        val discriminator = createInstructionDiscriminator("initialize")
        val instructionData = discriminator + argsBytes

        Log.d(TAG, "Instruction data details:")
        Log.d(TAG, "  argsBytes: ${argsBytes.contentToString()}")
        Log.d(TAG, "  argsBytes hex: ${argsBytes.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  discriminator: ${discriminator.contentToString()}")
        Log.d(TAG, "  discriminator hex: ${discriminator.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  instructionData: ${instructionData.contentToString()}")
        Log.d(TAG, "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  instructionData length: ${instructionData.size}")

        // 3. 创建账户列表 (AccountMeta)，顺序和属性必须与 solana_e_commerce.json 中的定义一致
        val accountMetas = listOf(
//            AccountMeta(SolanaPublicKey.from("CFbCBgv9NPZjcFao4zj8Dwkbhkv75Pw6djFaBmSibSR1"), true, true),
            AccountMeta(merchantPublicKey, true, true),
            AccountMeta(newAccount, true, true),
            AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )
        Log.d(TAG, "programId: ${programId.base58()}")
        accountMetas.forEachIndexed { index, meta ->
            Log.d(TAG, "accountMetas[$index]: pubkey=${meta.publicKey.base58()}, isSigner=${meta.isSigner}, isWritable=${meta.isWritable}")
        }
        // 4. 创建指令
        val instruction = genTransactionInstruction(programId, accountMetas, instructionData)

        // 5. 获取最新区块哈希并构建交易
        val recentBlockhash = try {
            kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                recentBlockhashUseCase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
            throw Exception("Failed to get recent blockhash: ${e.message}", e)
        }
        Log.d(TAG, "recentBlockhash: $recentBlockhash")
        val message = Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash).build()
        val transaction = Transaction(message)
        return transaction
    }

    private fun genTransactionInstruction( programId: SolanaPublicKey,
                                        accounts: List<AccountMeta>,
                                        data: ByteArray): TransactionInstruction {
        Log.d(TAG, "============genTransactionInstruction : $programId" )
        accounts.forEachIndexed { index, meta ->
            Log.d(TAG, "accountMetas[$index]: pubkey=${meta.publicKey.base58()}, isSigner=${meta.isSigner}, isWritable=${meta.isWritable}")
        }
        Log.d(TAG, "  instructionData: ${data.contentToString()}")
        Log.d(TAG, "  instructionData hex: ${data.joinToString("") { "%02x".format(it) }}")
        return TransactionInstruction(programId, accounts, data)
    }

    private suspend fun simpleTestTransaction(
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ):Transaction {
        val programId = SolanaPublicKey.from("96TkDXeRq7xGjmP1bzWn1kAVxukzpL1MsyajKX15fXyg")
        val merchantPublicKey = SolanaPublicKey.from(merchantRegistration.merchantPublicKey)
        // 随机生成一个 Ed25519 密钥对，并取公钥
        val keyGen = java.security.KeyPairGenerator.getInstance("Ed25519", org.bouncycastle.jce.provider.BouncyCastleProvider())
        val keyPair = keyGen.generateKeyPair()
        val publicKeyBytes = keyPair.public.encoded.takeLast(32).toByteArray()
        val newAccount = SolanaPublicKey(publicKeyBytes)

        // 2. 使用 AnchorInstructionSerializer 和 Borsh 对指令数据进行序列化
        //    指令名称 "register_merchant_atomic" 必须与 Anchor 合约中的方法名完全匹配。
        // 使用 borshj 进行序列化
        val argsBytes = Borsh.serialize(42L)
        val discriminator = createInstructionDiscriminator("test")
        val instructionData = discriminator + argsBytes

        Log.d(TAG, "Instruction data details:")
        Log.d(TAG, "  argsBytes: ${argsBytes.contentToString()}")
        Log.d(TAG, "  argsBytes hex: ${argsBytes.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  discriminator: ${discriminator.contentToString()}")
        Log.d(TAG, "  discriminator hex: ${discriminator.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  instructionData: ${instructionData.contentToString()}")
        Log.d(TAG, "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "  instructionData length: ${instructionData.size}")

        // 3. 创建账户列表 (AccountMeta)，顺序和属性必须与 solana_e_commerce.json 中的定义一致
        val accountMetas = listOf(
//            AccountMeta(SolanaPublicKey.from("CFbCBgv9NPZjcFao4zj8Dwkbhkv75Pw6djFaBmSibSR1"), true, true),
//            AccountMeta(newAccount, true, true),
            AccountMeta(merchantPublicKey, true, true),
            AccountMeta(SystemProgram.PROGRAM_ID, false, false)
        )
        // 4. 创建指令
        val instruction = genTransactionInstruction(programId, accountMetas, instructionData)

        // 5. 获取最新区块哈希并构建交易
        val recentBlockhash = try {
            kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                recentBlockhashUseCase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
            throw Exception("Failed to get recent blockhash: ${e.message}", e)
        }
        Log.d(TAG, "recentBlockhash: $recentBlockhash")
        val message = Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash).build()
        val transaction = Transaction(message)
        return transaction
    }

    /**
     * mock Solana Memo Program
     */
    private fun mockMemoTransaction(
        merchantPublicKey: SolanaPublicKey,
        recentBlockHash: SolanaPublicKey
    ): Transaction {
        val memoProgramId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val memoInstruction = genTransactionInstruction(
            memoProgramId,
            listOf(AccountMeta(merchantPublicKey, true, true)),
            "HelloFocxMemo".encodeToByteArray()
        )

        // Build Message
        val memoTxMessage = Message.Builder()
            .addInstruction(memoInstruction)
            .setRecentBlockhash(recentBlockHash)
            .build()
        return Transaction(memoTxMessage)
    }

     suspend fun registerMerchantAtomic5 (
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult  {
        return try {
            Log.d(TAG, "Starting transactionDemo, walletAdapter authToken: ${walletAdapter.authToken}")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "transactionDemo authResult.authToken:${authResult.authToken}")

                val programId = SolanaPublicKey.from("ADraQ2ENAbVoVZhvH5SPxWPsF2hH5YmFcgx61TafHuwu")
                val seeds = listOf("counter".encodeToByteArray())
                val pdaResult = ProgramDerivedAddress.find(seeds, programId)
                val counterAccountPDA = pdaResult.getOrNull()

                Log.d(TAG, "Calculated PDA:")
                Log.d(TAG, "  counterAccountPDA: ${counterAccountPDA?.base58()}")

                // 1. 构造指令参数
                val args = Args_increment(1u)

                // 2. 使用 borshj 进行序列化
                val argsBytes = Borsh.serialize(args)
                val discriminator = createInstructionDiscriminator("increment")
                val instructionData = discriminator + argsBytes

                Log.d(TAG, "Instruction data details:")
                Log.d(TAG, "  args: amount=${args.amount}")
                Log.d(TAG, "  argsBytes: ${argsBytes.contentToString()}")
                Log.d(TAG, "  argsBytes hex: ${argsBytes.joinToString("") { "%02x".format(it) }}")
                Log.d(TAG, "  discriminator: ${discriminator.contentToString()}")
                Log.d(TAG, "  discriminator hex: ${discriminator.joinToString("") { "%02x".format(it) }}")
                Log.d(TAG, "  instructionData: ${instructionData.contentToString()}")
                Log.d(TAG, "  instructionData hex: ${instructionData.joinToString("") { "%02x".format(it) }}")
                Log.d(TAG, "  instructionData length: ${instructionData.size}")

                // 3. 创建账户列表
                val accountMetas = listOf(
                    AccountMeta(counterAccountPDA!!, false, true), // counter account (writable)
                    AccountMeta(SystemProgram.PROGRAM_ID, false, false)
                )

                // 4. 创建指令
                val instruction = genTransactionInstruction(programId, accountMetas, instructionData)

                // 5. 获取最新区块哈希并构建交易
                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder().addInstruction(instruction).setRecentBlockhash(recentBlockhash).build()
                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (Demo): before")
                val signResult = signAndSendTransactions(arrayOf(transaction.serialize()))
                Log.d(TAG, "signAndSendTransactions (Demo): $signResult")
                signResult
            }

            // 处理交易结果
            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(TAG, "Demo transaction successful: ${Base58.encodeToString(signature)}")
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

    suspend fun registerMerchantAtomic4 (
        merchantRegistration: MerchantRegistration, activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult  {
        return try {
            Log.d(TAG, "Starting transactionDemo, walletAdapter authToken: ${walletAdapter.authToken}")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                Log.d(TAG, "transactionDemo authResult.authToken:${authResult.authToken}")

                val memoProgramId = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
                val memoProgramIdKey = SolanaPublicKey.from(memoProgramId)

                // Construct the instruction
                val memoInstruction = genTransactionInstruction(
                    memoProgramIdKey,
                    // Define the accounts in instruction
                    listOf(AccountMeta(SolanaPublicKey.from(merchantRegistration.merchantPublicKey), true, true)),
                    // Pass in the instruction data as ByteArray
                    "Hello Solana!".encodeToByteArray()
                )

                // 5. 获取最新区块哈希并构建交易
                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                Log.d(TAG, "recentBlockhash: $recentBlockhash")
                val message = Message.Builder().addInstruction(memoInstruction).setRecentBlockhash(recentBlockhash).build()
                val transaction = Transaction(message)

                Log.d(TAG, "signAndSendTransactions (Demo): before")
                val signResult = signAndSendTransactions(arrayOf(transaction.serialize()))
                Log.d(TAG, "signAndSendTransactions (Demo): $signResult")
                signResult
            }

            // 处理交易结果
            when (result) {
                is TransactionResult.Success -> {
                    val signature = result.successPayload?.signatures?.first()
                    if (signature != null) {
                        Log.d(TAG, "Demo transaction successful: ${Base58.encodeToString(signature)}")
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

    override suspend fun getMerchantStatus(walletAddress: String): Flow<MerchantStatus> = flow {
        try {
            // TODO: Implement real Solana network query
            // This should query the merchant account data from the Solana blockchain
            throw RuntimeException("getMerchantStatus requires real Solana network implementation")
        } catch (e: Exception) {
            emit(
                MerchantStatus(
                    isRegistered = false,
                    merchantAccount = null,
                    registrationDate = null,
                    securityDeposit = null,
                    status = AppConstants.Merchant.DEFAULT_STATUS
                )
            )
        }
    }

    override suspend fun getMerchantAccountData(merchantAccount: String): Flow<MerchantStatus> = flow {
        try {
            Log.d(TAG, "Querying merchant account data for: $merchantAccount")

            val merchantPublicKey = SolanaPublicKey.from(merchantAccount)
            val programId = SolanaPublicKey.from(AppConstants.App.PROGRAM_ID)

            // Calculate merchant info PDA
            val merchantInfoPda = SolanaTokenUtils.findProgramAddress(
                listOf("merchant_info".toByteArray(), merchantPublicKey.bytes), programId.bytes
            )

            // Calculate merchant ID account PDA
            val merchantIdAccountPda = SolanaTokenUtils.findProgramAddress(
                listOf("merchant".toByteArray(), merchantPublicKey.bytes), programId.bytes
            )

            Log.d(TAG, "Calculated PDAs:")
            Log.d(TAG, "  merchantInfoPda: ${merchantInfoPda.first.base58()}")
            Log.d(TAG, "  merchantIdAccountPda: ${merchantIdAccountPda.first.base58()}")

            // Query account info from Solana network using SolanaRpcClient
            val rpcUri = Uri.parse(NetworkConfig.getRpcUrl())

            // Check if merchant info account exists
            val merchantInfoAccountInfo = queryAccountInfo(rpcUri, merchantInfoPda.first.base58())
            val merchantIdAccountInfo = queryAccountInfo(rpcUri, merchantIdAccountPda.first.base58())

            if (merchantInfoAccountInfo != null && merchantIdAccountInfo != null) {
                // Parse account data to extract merchant information
                val merchantStatus = parseMerchantAccountData(
                    merchantInfoAccountInfo, merchantIdAccountInfo, merchantAccount
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
                        status = AppConstants.Merchant.DEFAULT_STATUS
                    )
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error querying merchant account data: ${e.message}", e)
            emit(
                MerchantStatus(
                    isRegistered = false,
                    merchantAccount = null,
                    registrationDate = null,
                    securityDeposit = null,
                    status = AppConstants.Merchant.DEFAULT_STATUS
                )
            )
        }
    }


    /**
     * Query account info from Solana RPC using SolanaRpcClient
     */
    private suspend fun queryAccountInfo(rpcUri: Uri, accountAddress: String): AccountInfo<ByteArray>? {
        return try {
            val publicKey = SolanaPublicKey.from(accountAddress)
            val rpcResponse = solanaRpcClient.getAccountInfo(publicKey)
            rpcResponse.result
        } catch (e: Exception) {
            Log.e(TAG, "Error querying account info for $accountAddress: ${e.message}", e)
            null
        }
    }

    /**
     * Parse merchant account data from Solana account info
     */
    private fun parseMerchantAccountData(
        merchantInfoAccountInfo: AccountInfo<ByteArray>?,
        merchantIdAccountInfo: AccountInfo<ByteArray>?,
        merchantAccount: String
    ): MerchantStatus {
        try {
            // Extract account data (base64 encoded)
            if (merchantInfoAccountInfo != null && merchantIdAccountInfo != null) {
                // Parse the account data based on your programs data structure
                val merchantInfoData = merchantInfoAccountInfo.data
                val merchantIdData = merchantIdAccountInfo.data

                Log.d(TAG, "Merchant info data: ${merchantInfoData?.contentToString()}")
                Log.d(TAG, "Merchant ID data: ${merchantIdData?.contentToString()}")

                // For now, return a basic registered status
                // You should implement proper deserialization based on your Solana program's account structure
                return MerchantStatus(
                    isRegistered = true,
                    merchantAccount = merchantAccount,
                    registrationDate = System.currentTimeMillis().toString(), // You should parse this from account data
                    securityDeposit = merchantInfoAccountInfo.lamports?.toString(), // Use lamports as security deposit
                    status = "active" // You should parse this from account data
                )
            }

            return MerchantStatus(
                isRegistered = false,
                merchantAccount = null,
                registrationDate = null,
                securityDeposit = null,
                status = AppConstants.Merchant.DEFAULT_STATUS
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing merchant account data: ${e.message}", e)
            return MerchantStatus(
                isRegistered = false,
                merchantAccount = null,
                registrationDate = null,
                securityDeposit = null,
                status = AppConstants.Merchant.DEFAULT_STATUS
            )
        }
    }

    override suspend fun depositMerchantFunds(
        merchantAccount: String,
        depositAmount: Long,
        activityResultSender: ActivityResultSender
    ): MerchantRegistrationResult {
        return try {
            Log.d(TAG, "Starting depositMerchantFunds for merchant: $merchantAccount, amount: $depositAmount")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val merchantPublicKey = SolanaPublicKey.from(merchantAccount)
                val authorityPublicKey = SolanaPublicKey.from(AppConstants.App.AUTHORITY_PUBLIC_KEY)
                val programId = SolanaPublicKey.from(AppConstants.App.PROGRAM_ID)

                // Calculate PDAs
                val merchantInfoPda = SolanaTokenUtils.findProgramAddress(
                    listOf("merchant_info".toByteArray(), merchantPublicKey.bytes),
                    programId.bytes
                )

                val systemConfigPda = SolanaTokenUtils.findProgramAddress(
                    listOf("system_config".toByteArray()),
                    programId.bytes
                )

                // Calculate USDC token account addresses using Associated Token Account (ATA)
                val usdcMint = SolanaTokenUtils.TokenMints.USDC
                val tokenProgramId = SolanaTokenUtils.ProgramIds.TOKEN_PROGRAM

                // Calculate merchant's USDC Associated Token Account
                val merchantUsdcAccount = SolanaTokenUtils.getUsdcAssociatedTokenAccount(merchantPublicKey)

                // Calculate program's USDC Associated Token Account
                val programUsdcAccount = SolanaTokenUtils.getUsdcAssociatedTokenAccount(programId)

                Log.d(TAG, "Calculated PDAs for deposit:")
                Log.d(TAG, "  merchantInfoPda: ${merchantInfoPda.first.base58()}")
                Log.d(TAG, "  systemConfigPda: ${systemConfigPda.first.base58()}")
                Log.d(TAG, "  merchantUsdcAccount: ${merchantUsdcAccount.base58()}")
                Log.d(TAG, "  programUsdcAccount: ${programUsdcAccount.base58()}")
                Log.d(TAG, "  usdcMint: ${usdcMint.base58()}")

                // Create instruction data for depositMerchantFunds
                val instructionData = createDepositMerchantFundsInstructionData(depositAmount)

                // Create account metas for the instruction
                val accountMetas = listOf(
                    AccountMeta(merchantPublicKey, true, true), // merchant (signer, writable)
                    AccountMeta(authorityPublicKey, true, false), // authority (signer)
                    AccountMeta(merchantInfoPda.first, false, true), // merchantInfo (writable)
                    AccountMeta(systemConfigPda.first, false, true), // systemConfig (writable)
                    AccountMeta(merchantUsdcAccount, false, true), // merchantUsdcAccount (writable)
                    AccountMeta(programUsdcAccount, false, true), // programUsdcAccount (writable)
                    AccountMeta(tokenProgramId, false, false) // tokenProgram
                )

                // Create the instruction
                val instruction = genTransactionInstruction(
                    programId, accountMetas, instructionData
                )

                // Get recent blockhash and create transaction
                val recentBlockhash = try {
                    kotlinx.coroutines.withTimeout(NetworkConfig.READ_TIMEOUT_MS) {
                        recentBlockhashUseCase()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get recent blockhash for deposit: ${e.message}", e)
                    throw Exception("Failed to get recent blockhash: ${e.message}", e)
                }

                val message = Message.Builder()
                    .addInstruction(instruction)
                    .setRecentBlockhash(recentBlockhash)
                    .build()
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
        // Instruction discriminator for depositMerchantFunds method
        // This should match the method hash in your Anchor program
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

        // Serialize deposit amount as u64 (8 bytes, little endian)
        val amountBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(depositAmount).array()

        // Combine discriminator and amount
        return discriminator + amountBytes
    }

    /**
     * Create instruction data for registerMerchantAtomic method
     * Based on the Anchor program method signature
     */
    private fun createRegisterMerchantAtomicInstructionData(
        merchantName: String, merchantDescription: String
    ): ByteArray {
        // Instruction discriminator for registerMerchantAtomic method
        // This should match the method hash in your Anchor program
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

        // Serialize merchant name
        val nameBytes = merchantName.toByteArray(Charsets.UTF_8)
        val nameLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(nameBytes.size).array()

        // Serialize merchant description
        val descriptionBytes = merchantDescription.toByteArray(Charsets.UTF_8)
        val descriptionLength =
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(descriptionBytes.size).array()

        // Combine all data
        return discriminator + nameLength + nameBytes + descriptionLength + descriptionBytes
    }

    private fun registerWithAnchor() {
        // TODO anchor: 此函数是您请求创建的占位符。
        // 上面的 `registerMerchantWithAnchor` 方法是完整的实现，您可以直接调用它。
        // 如果您希望在此处编写独立的调用逻辑，请参考 `registerMerchantWithAnchor` 的实现。
        val programId = SolanaPublicKey.from("ADraQ2ENAbVoVZhvH5SPxWPsF2hH5YmFcgx61TafHuwu")
    }
}