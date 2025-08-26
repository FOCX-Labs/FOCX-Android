package com.focx.domain.usecase

import com.focx.core.constants.AppConstants
import com.focx.core.network.NetworkConnectionManager
import com.focx.utils.Log
import com.focx.utils.ShopUtils
import com.solana.publickey.SolanaPublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaTokenBalanceUseCase @Inject constructor(
    private val networkConnectionManager: NetworkConnectionManager
) {
    private val TAG = SolanaTokenBalanceUseCase::class.simpleName

    suspend fun getBalance(address: SolanaPublicKey): Long {
        return try {
            val address58 = address.base58()
            val rpcResponse = networkConnectionManager.getSolanaRpcClient().getAccountInfo(address)
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

    /**
     * 获取指定owner和mint的token余额
     * @param owner 用户钱包地址
     * @param mint token mint地址，默认为应用默认的USDC mint
     * @return token余额
     */
    suspend fun getBalanceByOwnerAndMint(
        owner: SolanaPublicKey,
        mint: SolanaPublicKey = AppConstants.App.getMint()
    ): Long {
        return try {
            // 获取关联的token账户地址
            val tokenAccountAddress = ShopUtils.getAssociatedTokenAddress(owner, mint).getOrNull()
                ?: throw InvalidAccountException("Could not derive associated token address for owner [${owner.base58()}] and mint [${mint.base58()}]")
            
            // 获取token余额
            getBalance(tokenAccountAddress)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching token balance for owner [${owner.base58()}] and mint [${mint.base58()}]: ${e.message}", e)
            throw InvalidAccountException("Could not fetch token balance for owner [${owner.base58()}] and mint [${mint.base58()}]: ${e.message}", e)
        }
    }

    class InvalidAccountException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
}