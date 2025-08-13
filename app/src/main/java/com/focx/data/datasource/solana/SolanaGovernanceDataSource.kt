package com.focx.data.datasource.solana

import com.focx.core.constants.AppConstants

import com.focx.domain.entity.CreateProposalArgs
import com.focx.domain.entity.Dispute
import com.focx.domain.entity.DisputeStatus
import com.focx.domain.entity.GovernanceStats
import com.focx.domain.entity.InitiateDisputeArgs
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.domain.entity.ProposalStatus
import com.focx.domain.entity.ProposalType
import com.focx.domain.entity.Vote
import com.focx.domain.entity.VoteOnProposalArgs
import com.focx.domain.entity.VoteType
import com.focx.domain.repository.IGovernanceRepository
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.utils.GovernanceUtils
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.focx.utils.Utils
import com.funkatronics.encoders.Base58
import com.funkatronics.kborsh.Borsh
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.programs.SystemProgram
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import com.solana.serialization.AnchorInstructionSerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message.Builder
import com.solana.transaction.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaGovernanceDataSource @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val recentBlockhashUseCase: RecentBlockhashUseCase,
    private val solanaRpcClient: SolanaRpcClient
) : IGovernanceRepository {

    companion object {

        private val TAG = "SGDS"
    }

    override suspend fun getProposals(): Flow<List<Proposal>> = flow {
        emit(emptyList()) // TODO: Implement blockchain governance queries
    }

    override suspend fun getProposals(page: Int, pageSize: Int): Flow<List<Proposal>> = flow {
        try {
            val proposals = GovernanceUtils.getProposalList(solanaRpcClient, page, pageSize)
            emit(proposals)
        } catch (e: Exception) {
            Log.e(
                "SolanaGovernance",
                "Failed to get proposals from blockchain",
                e
            )
            emit(emptyList())
        }
    }

    override suspend fun getProposalById(id: String): Proposal? {
        return null // TODO: Implement blockchain governance queries
    }

    override suspend fun getActiveProposals(): Flow<List<Proposal>> = flow {
        try {
            val proposals = GovernanceUtils.getProposalList(solanaRpcClient)
            emit(proposals.filter { it.status == ProposalStatus.PENDING })
        } catch (e: Exception) {
            Log.e(
                "SolanaGovernance",
                "Failed to get active proposals from blockchain",
                e
            )
            emit(emptyList())
        }
    }

    override suspend fun getActiveProposals(page: Int, pageSize: Int): Flow<List<Proposal>> = flow {
        try {
            val proposals = GovernanceUtils.getProposalList(solanaRpcClient, page, pageSize)
            emit(proposals.filter { it.status == ProposalStatus.PENDING })
        } catch (e: Exception) {
            Log.e(
                "SolanaGovernance",
                "Failed to get active proposals from blockchain",
                e
            )
            emit(emptyList())
        }
    }

    override suspend fun getGovernanceStats(): Flow<GovernanceStats> = flow {
        try {
            val configPda = GovernanceUtils.getGovernanceConfigPda()
            Log.d(TAG, "$configPda")
            

            
            // Use manual parsing function to handle committeeMembers fixed-length array
            val config = GovernanceUtils.getGovernanceConfigManual(solanaRpcClient)
            Log.d(TAG, "config $config")
            val totalPower = GovernanceUtils.getTotalVotingPower(config, solanaRpcClient)
            emit(
                GovernanceStats(
                    config?.proposalCounter ?: 0UL,
                    0,
                    0,
                    0.0,
                    totalPower.toDouble(),
                    0.0
                )
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to get governance stats from blockchain, using mock data",
                e
            )
            // Fallback to empty stats
            emit(
                GovernanceStats(
                    activeProposals = 0UL,
                    totalProposals = 0,
                    totalVotes = 0,
                    passRate = 0.0,
                    totalVotingPower = 0.0,
                    participationRate = 0.0
                )
            )
        }
    }

    override suspend fun createProposal(
        title: String,
        description: String,
        proposalType: ProposalType,
        proposerPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Starting createProposal: $title")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val builder = Builder()

                val config = GovernanceUtils.getGovernanceConfigManual(solanaRpcClient)!!
                val proposalId = config.proposalCounter + 1UL
                val proposalPda = GovernanceUtils.getProposalPda(proposalId)
                val governancePda = GovernanceUtils.getGovernanceConfigPda()
                val governanceTokenVault = GovernanceUtils.getGovernanceTokenVault()
                val proposerUsdcAccount = ShopUtils.getAssociatedTokenAddress(proposerPubKey).getOrNull()!!

                val ix = ShopUtils.genTransactionInstruction(
                    listOf(
                        AccountMeta(proposalPda, false, true),
                        AccountMeta(governancePda, false, true),
                        AccountMeta(proposerPubKey, true, true),
                        AccountMeta(proposerUsdcAccount, false, true),
                        AccountMeta(governanceTokenVault, false, true),
                        AccountMeta(SystemProgram.PROGRAM_ID, false, false),
                        AccountMeta(
                            SolanaPublicKey.from(AppConstants.App.SPL_TOKEN_PROGRAM_ID),
                            false,
                            false
                        ),
                    ),
                    Borsh.encodeToByteArray(
                        AnchorInstructionSerializer("create_proposal"),
                        CreateProposalArgs(
                            title = title,
                            description = description,
                            proposalType = proposalType,
                            executionData = null,
                            customDepositRaw = null,
                        )
                    ),
                    AppConstants.App.getGovernanceProgramId()
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
                        Log.d(
                            TAG,
                            "Proposal created successfully: ${Base58.encodeToString(signature)}"
                        )

                        Result.success(Unit)
                    } else {
                        Log.e(
                            TAG,
                            "No signature returned from createProposal transaction"
                        )
                        Result.failure(Exception("No signature returned from transaction"))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for createProposal")
                    Result.failure(Exception("No wallet found"))
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "createProposal failed: ${result.e.message}")
                    Result.failure(Exception("Transaction failed: ${result.e.message}"))
                }

                else -> Result.failure(Exception("Unknown transaction result"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createProposal exception:", e)
            Result.failure(Exception("Failed to create proposal: ${e.message}"))
        }
    }

    override suspend fun voteOnProposal(
        proposalId: ULong,
        voteType: VoteType,
        voterPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Starting voteOnProposal: $proposalId, voteType: $voteType")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val builder = Builder()

                // Generate vote instruction
                val config = GovernanceUtils.getGovernanceConfigManual(solanaRpcClient)
                val proposalPda = GovernanceUtils.getProposalPda(proposalId)
                val governancePda = GovernanceUtils.getGovernanceConfigPda()
                val votePda = GovernanceUtils.getVotePda(proposalId, voterPubKey)
                val committeeTokenMint = config?.committeeTokenMint ?: AppConstants.App.getMint()
                val voterTokenAccount = ShopUtils.getAssociatedTokenAddress(voterPubKey, committeeTokenMint).getOrNull()!!

                val ix = ShopUtils.genTransactionInstruction(
                    listOf(
                        AccountMeta(proposalPda, false, true),
                        AccountMeta(votePda, false, true),
                        AccountMeta(governancePda, false, false),
                        AccountMeta(voterPubKey, true, true),
                        AccountMeta(voterTokenAccount, false, false),
                        AccountMeta(committeeTokenMint, false, false),
                        AccountMeta(SystemProgram.PROGRAM_ID, false, false),
                    ),
                    Borsh.encodeToByteArray(
                        AnchorInstructionSerializer("cast_vote"),
                        VoteOnProposalArgs(
                            proposalId = proposalId,
                            voteType = voteType
                        )
                    ),
                    AppConstants.App.getGovernanceProgramId()
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
                        Log.d(
                            TAG,
                            "Vote submitted successfully: ${Base58.encodeToString(signature)}"
                        )

                        Result.success(Unit)
                    } else {
                        Log.e(
                            TAG,
                            "No signature returned from voteOnProposal transaction"
                        )
                        Result.failure(Exception("No signature returned from transaction"))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for voteOnProposal")
                    Result.failure(Exception("No wallet found"))
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "voteOnProposal failed: ${result.e.message}")
                    Result.failure(Exception("Transaction failed: ${result.e.message}"))
                }

                else -> Result.failure(Exception("Unknown transaction result"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "voteOnProposal exception:", e)
            Result.failure(Exception("Failed to vote on proposal: ${e.message}"))
        }
    }

    override suspend fun getVotesByProposal(proposalId: String): Flow<List<Vote>> = flow {
        emit(emptyList()) // TODO: Implement blockchain governance queries
    }

    override suspend fun getVotesByUser(userId: String): Flow<List<Vote>> = flow {
        emit(emptyList()) // TODO: Implement blockchain governance queries
    }

    override suspend fun getUserVotingPower(userId: String): Double {
        return 0.0 // TODO: Implement blockchain governance queries
    }

    override suspend fun getDisputes(): Flow<List<Dispute>> = flow {
        try {
            // TODO: Implement blockchain governance queries
            emit(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get disputes from blockchain", e)
            emit(emptyList())
        }
    }

    override suspend fun getPlatformRules(): Flow<List<PlatformRule>> = flow {
        emit(
            listOf(
                PlatformRule(
                    "Product Listings", listOf(
                        "No counterfeit or replica products",
                        "Accurate product descriptions required",
                        "Real product images only",
                        "Proper categorization mandatory"
                    )
                ), PlatformRule(
                    "Trading Conduct", listOf(
                        "Honor all confirmed orders",
                        "Ship within stated timeframe",
                        "Provide tracking information",
                        "Respond to buyer inquiries within 24 hours"
                    )
                ), PlatformRule(
                    "Prohibited Items", listOf(
                        "Illegal substances and weapons",
                        "Stolen goods",
                        "Adult content",
                        "Hazardous materials"
                    )
                )
            )
        )
    }

    override suspend fun voteOnDispute(disputeId: String, favorBuyer: Boolean): Result<Unit> {
        return Result.failure(Exception("Not implemented")) // TODO: Implement blockchain governance
    }

    override suspend fun initiateDispute(
        orderId: String,
        buyerPubKey: SolanaPublicKey,
        activityResultSender: ActivityResultSender
    ): Result<Dispute> {
        return try {
            Log.d(TAG, "Starting initiateDispute for order: $orderId")

            val result = walletAdapter.transact(activityResultSender) { authResult ->
                val builder = Builder()

                // Generate dispute initiation instruction
                val orderPda = SolanaPublicKey.from(orderId)
                val disputePda = ShopUtils.getSimplePda("dispute_$orderId").getOrNull()!!
                val governancePda = ShopUtils.getSimplePda("governance").getOrNull()!!

                val ix = ShopUtils.genTransactionInstruction(
                    listOf(
                        AccountMeta(disputePda, false, true),
                        AccountMeta(orderPda, false, false),
                        AccountMeta(governancePda, false, true),
                        AccountMeta(buyerPubKey, true, true),
                        AccountMeta(SystemProgram.PROGRAM_ID, false, false),
                    ),
                    Borsh.encodeToByteArray(
                        AnchorInstructionSerializer("initiate_dispute"),
                        InitiateDisputeArgs(
                            orderId = orderId
                        )
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
                        Log.d(
                            TAG,
                            "Dispute initiated successfully: ${Base58.encodeToString(signature)}"
                        )

                        val newDispute = Dispute(
                            id = "dispute_${System.currentTimeMillis()}",
                            title = "Order Dispute $orderId",
                            buyer = buyerPubKey.toString(),
                            order = orderId,
                            amount = "299.99 USDC", // TODO: Get actual order amount
                            submitted = SimpleDateFormat(
                                "MM/dd/yyyy",
                                Locale.US
                            ).format(Date()),
                            status = DisputeStatus.UNDER_REVIEW,
                            daysRemaining = 7,
                            evidenceSummary = "Buyer initiated dispute for order $orderId",
                            communityVoting = null
                        )

                        Result.success(newDispute)
                    } else {
                        Log.e(
                            TAG,
                            "No signature returned from initiateDispute transaction"
                        )
                        Result.failure(Exception("No signature returned from transaction"))
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "No wallet found for initiateDispute")
                    Result.failure(Exception("No wallet found"))
                }

                is TransactionResult.Failure -> {
                    Log.e(TAG, "initiateDispute failed: ${result.e.message}")
                    Result.failure(Exception("Transaction failed: ${result.e.message}"))
                }

                else -> Result.failure(Exception("Unknown transaction result"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "initiateDispute exception:", e)
            Result.failure(Exception("Failed to initiate dispute: ${e.message}"))
        }
    }
}