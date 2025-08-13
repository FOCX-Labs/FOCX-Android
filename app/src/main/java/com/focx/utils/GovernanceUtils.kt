package com.focx.utils

import com.focx.core.constants.AppConstants
import com.focx.domain.entity.GovernanceConfig
import com.focx.domain.entity.Proposal
import com.funkatronics.encoders.Base58
import com.funkatronics.kborsh.Borsh
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.getAccountInfo
import kotlinx.serialization.encodeToByteArray
import kotlin.math.max
import kotlin.math.min

object GovernanceUtils {
    private const val TAG = "GovernanceUtils"
    private const val COMMITTEE_MEMBERS_MAX_SIZE = 10
    
    suspend fun getGovernanceConfigPda(): SolanaPublicKey {
        return ProgramDerivedAddress.find(
            listOf(
                "governance_config".toByteArray(),
            ),
            AppConstants.App.getGovernanceProgramId()
        ).getOrNull()!!
    }

    suspend fun getProposalPda(proposalId: ULong): SolanaPublicKey {
        return ProgramDerivedAddress.find(
            listOf(
                "proposal".toByteArray(),
                Borsh.encodeToByteArray(proposalId)
            ),
            AppConstants.App.getGovernanceProgramId()
        ).getOrNull()!!
    }

    suspend fun getGovernanceConfig(solanaRpcClient: SolanaRpcClient): GovernanceConfig? {
        val data =
            solanaRpcClient.getAccountInfo<GovernanceConfig>(getGovernanceConfigPda()).result?.data
        Log.d(TAG, "getGovernanceConfig: $data")
        return data
    }

    /**
     * Manually parse GovernanceConfig, handling committeeMembers fixed-length array
     */
    suspend fun getGovernanceConfigManual(solanaRpcClient: SolanaRpcClient): GovernanceConfig? {
        try {
            val configPda = getGovernanceConfigPda()
            val accountInfo = solanaRpcClient.getAccountInfo(configPda).result
            
            if (accountInfo?.data == null) {
                Log.w(TAG, "Governance config account not found or data is null")
                return null
            }
            
            val data = accountInfo.data!!
            Log.d(TAG, "Raw governance config data size: ${data.size}")
            
            // Manually parse byte array
            return parseGovernanceConfigFromBytes(data)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error manually parsing governance config: ${e.message}", e)
            return null
        }
    }

    /**
     * Manually parse GovernanceConfig from byte array
     * Data structure:
     * - discriminator: 8 bytes
     * - authority: 32 bytes (SolanaPublicKey)
     * - committeeTokenMint: 32 bytes (SolanaPublicKey)
     * - committeeMembers: 32 * 10 bytes (fixed-length array, maximum 10 members)
     * - committeeMemberCount: 1 byte (UByte)
     * - proposalDeposit: 8 bytes (ULong)
     * - votingPeriod: 8 bytes (ULong)
     * - participationThreshold: 2 bytes (UShort)
     * - approvalThreshold: 2 bytes (UShort)
     * - vetoThreshold: 2 bytes (UShort)
     * - feeRate: 2 bytes (UShort)
     * - totalVotingPower: 8 bytes (ULong)
     * - proposalCounter: 8 bytes (ULong)
     * - createdAt: 8 bytes (Long)
     * - updatedAt: 8 bytes (Long)
     * - testMode: 1 byte (Boolean)
     * - bump: 1 byte (UByte)
     */
    private fun parseGovernanceConfigFromBytes(data: ByteArray): GovernanceConfig {
        var offset = 0
        
        // Check if data size is sufficient
        val expectedSize = 8 + 32 + 32 + (33 * COMMITTEE_MEMBERS_MAX_SIZE) + 1 + 8 + 8 + 2 + 2 + 2 + 2 + 8 + 8 + 8 + 8 + 1 + 1
        if (data.size < expectedSize) {
            Log.w(TAG, "Governance config data size too small: ${data.size}, expected: $expectedSize")
        }
        
        // Parse discriminator (8 bytes) - little-endian
        val discriminator = data.slice(offset until offset + 8).foldIndexed(0L) { index, acc, byte ->
            acc or ((byte.toLong() and 0xFF) shl (index * 8))
        }
        offset += 8
        
        // Parse authority (32 bytes)
        val authorityBytes = data.slice(offset until offset + 32).toByteArray()
        val authority = SolanaPublicKey.from(Base58.encodeToString(authorityBytes))
        offset += 32
        
        // Parse committeeTokenMint (32 bytes)
        val committeeTokenMintBytes = data.slice(offset until offset + 32).toByteArray()
        val committeeTokenMint = SolanaPublicKey.from(Base58.encodeToString(committeeTokenMintBytes))
        offset += 32
        
        // Parse committeeMembers (1 + 32) * 10 bytes = 330 bytes for option<pubkey> array
        val committeeMembers = mutableListOf<SolanaPublicKey?>()
        for (i in 0 until COMMITTEE_MEMBERS_MAX_SIZE) {
            val optionFlag = data[offset] // 1 byte option flag
            offset += 1
            if (optionFlag == 0.toByte()) {
                committeeMembers.add(null) // None
            } else {
                val memberBytes = data.slice(offset until offset + 32).toByteArray()
                val member = SolanaPublicKey.from(Base58.encodeToString(memberBytes))
                committeeMembers.add(member)
                offset += 32
            }
        }
        
        // Parse committeeMemberCount (1 byte) - UByte
        val committeeMemberCount = data[offset].toUByte()
        offset += 1
        
        // Parse proposalDeposit (8 bytes) - little-endian
        val proposalDeposit = data.slice(offset until offset + 8).foldIndexed(0UL) { index, acc, byte ->
            acc or ((byte.toULong() and 0xFFu) shl (index * 8))
        }
        offset += 8
        
        // Parse votingPeriod (8 bytes) - little-endian
        val votingPeriod = data.slice(offset until offset + 8).foldIndexed(0UL) { index, acc, byte ->
            acc or ((byte.toULong() and 0xFFu) shl (index * 8))
        }
        offset += 8
        
        // Parse participationThreshold (2 bytes) - little-endian
        val participationThreshold = data.slice(offset until offset + 2).foldIndexed(0) { index, acc, byte ->
            acc or ((byte.toInt() and 0xFF) shl (index * 8))
        }.toUShort()
        offset += 2
        
        // Parse approvalThreshold (2 bytes) - little-endian
        val approvalThreshold = data.slice(offset until offset + 2).foldIndexed(0) { index, acc, byte ->
            acc or ((byte.toInt() and 0xFF) shl (index * 8))
        }.toUShort()
        offset += 2
        
        // Parse vetoThreshold (2 bytes) - little-endian
        val vetoThreshold = data.slice(offset until offset + 2).foldIndexed(0) { index, acc, byte ->
            acc or ((byte.toInt() and 0xFF) shl (index * 8))
        }.toUShort()
        offset += 2
        
        // Parse feeRate (2 bytes) - little-endian
        val feeRate = data.slice(offset until offset + 2).foldIndexed(0) { index, acc, byte ->
            acc or ((byte.toInt() and 0xFF) shl (index * 8))
        }.toUShort()
        offset += 2
        
        // Parse totalVotingPower (8 bytes) - little-endian
        val totalVotingPower = data.slice(offset until offset + 8).foldIndexed(0UL) { index, acc, byte ->
            acc or ((byte.toULong() and 0xFFu) shl (index * 8))
        }
        offset += 8
        
        // Parse proposalCounter (8 bytes) - little-endian
        val proposalCounter = data.slice(offset until offset + 8).foldIndexed(0UL) { index, acc, byte ->
            acc or ((byte.toULong() and 0xFFu) shl (index * 8))
        }
        offset += 8
        
        // Parse createdAt (8 bytes) - little-endian
        val createdAt = data.slice(offset until offset + 8).foldIndexed(0L) { index, acc, byte ->
            acc or ((byte.toLong() and 0xFF) shl (index * 8))
        }
        offset += 8
        
        // Parse updatedAt (8 bytes) - little-endian
        val updatedAt = data.slice(offset until offset + 8).foldIndexed(0L) { index, acc, byte ->
            acc or ((byte.toLong() and 0xFF) shl (index * 8))
        }
        offset += 8
        
        // Parse testMode (1 byte)
        val testMode = data[offset] != 0.toByte()
        offset += 1
        
        // Parse bump (1 byte)
        val bump = data[offset].toUByte()
        
        val config = GovernanceConfig(
            discriminator = discriminator,
            authority = authority,
            committeeTokenMint = committeeTokenMint,
            committeeMembers = committeeMembers,
            committeeMemberCount = committeeMemberCount,
            proposalDeposit = proposalDeposit,
            votingPeriod = votingPeriod,
            participationThreshold = participationThreshold,
            approvalThreshold = approvalThreshold,
            vetoThreshold = vetoThreshold,
            feeRate = feeRate,
            totalVotingPower = totalVotingPower,
            proposalCounter = proposalCounter,
            createdAt = createdAt,
            updatedAt = updatedAt,
            testMode = testMode,
            bump = bump
        )

        return config
    }

    suspend fun getTotalVotingPower(
        config: GovernanceConfig?,
        solanaRpcClient: SolanaRpcClient
    ): ULong {
        if (config == null) {
            return 0UL
        }
        val commiteeMint = config.committeeTokenMint
        val members = config.committeeMembers

        var totalPower = 0UL
        for (m in members) {
            if (m == null) {
                continue
            }

            totalPower += Utils.getBalanceByOwnerAndMint(solanaRpcClient, m, commiteeMint)
        }

        return totalPower
    }

    suspend fun getProposalDetail(proposalId: ULong, solanaRpcClient: SolanaRpcClient): Proposal? {
        val data = solanaRpcClient.getAccountInfo<Proposal>(getProposalPda(proposalId)).result?.data
        Log.d(TAG, "getProposalDetail: $data")
        return data
    }

    suspend fun getProposalList(
        solanaRpcClient: SolanaRpcClient,
        page: Int = 1,
        pageSize: Int = 10
    ): List<Proposal> {
        val config = getGovernanceConfigManual(solanaRpcClient)
        val totalCount = config?.proposalCounter ?: 0UL
        
        if (totalCount == 0UL) {
            return emptyList()
        }
        
        // Calculate pagination info, fixed DESC sorting
        val startIndex = max(1, totalCount.toInt() - page * pageSize + 1)
        val endIndex = min(totalCount.toInt(), startIndex + pageSize - 1)
        
        if (totalCount.toInt() < startIndex) {
            return emptyList()
        }
        
        val result = ArrayList<Proposal>()
        for (i in startIndex..endIndex) {
            try {
                val proposal = getProposalDetail(i.toULong(), solanaRpcClient)
                if (proposal == null) {
                    Log.d(TAG, "proposal id $i can not found detail data")
                } else {
                    result.add(proposal)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting proposal $i", e)
            }
        }
        
        // Sort by DESC (latest first)
        result.reverse()
        
        return result
    }
}