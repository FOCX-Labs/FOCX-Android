package com.focx.utils

import com.focx.core.constants.AppConstants
import com.focx.domain.usecase.SolanaTokenBalanceUseCase.InvalidAccountException
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import kotlin.collections.slice

object Utils {

    private const val TAG = "Utils"

    suspend fun getBalanceByOwnerAndMint(
        solanaRpcClient: SolanaRpcClient,
        owner: SolanaPublicKey,
        mint: SolanaPublicKey = AppConstants.App.getMint()
    ): ULong {
        return try {
            // 获取关联的token账户地址
            val tokenAccountAddress = ShopUtils.getAssociatedTokenAddress(owner, mint).getOrNull()
                ?: throw InvalidAccountException("Could not derive associated token address for owner [${owner.base58()}] and mint [${mint.base58()}]")

            // 获取token余额
            getBalance(tokenAccountAddress, solanaRpcClient).toULong()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching token balance for owner [${owner.base58()}] and mint [${mint.base58()}]: ${e.message}", e)
            throw InvalidAccountException("Could not fetch token balance for owner [${owner.base58()}] and mint [${mint.base58()}]: ${e.message}", e)
        }
    }

    suspend fun getBalance(address: SolanaPublicKey, solanaRpcClient: SolanaRpcClient): Long {
        return try {
            val address58 = address.base58()
            val rpcResponse = solanaRpcClient.getAccountInfo(address)
            val accountInfo = rpcResponse.result

            if (accountInfo == null) {
                Log.w(TAG, "Token account not found: $address58")
                return 0L
            }

            val data = accountInfo.data
            if (data == null) {
                Log.w(TAG, "Token account data is null: $address58")
                return 0L
            }

            // Token账户的数据结构：前8字节是mint地址，接下来8字节是owner地址，然后是8字节的余额
            if (data.size < 72) {
                Log.w(TAG, "Invalid token account data size: ${data.size}")
                return 0L
            }

            // 解析余额（从第64字节开始的8字节）
            val balanceBytes = data.slice(64..71)
            val balance = balanceBytes.foldIndexed(0L) { index, acc, byte ->
                acc or ((byte.toLong() and 0xFF) shl (index * 8))
            }

            Log.d(TAG, "getTokenBalance pubKey=${address58}, balance=$balance")
            balance
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching token balance for account [${address.base58()}]: ${e.message}", e)
            throw InvalidAccountException("Could not fetch token balance for account [${address.base58()}]: ${e.message}", e)
        }
    }
}