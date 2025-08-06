package com.focx.domain.usecase

import com.focx.domain.entity.VaultDepositor
import com.focx.domain.entity.Transaction
import com.focx.domain.entity.WalletBalance
import com.focx.domain.repository.IWalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetWalletBalanceUseCase @Inject constructor(
    private val walletRepository: IWalletRepository
) {
    suspend operator fun invoke(address: String): Flow<Result<WalletBalance>> {
        return walletRepository.getBalance(address)
            .map { balance ->
                Result.success(balance)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class GetTransactionHistoryUseCase @Inject constructor(
    private val walletRepository: IWalletRepository
) {
    suspend operator fun invoke(address: String): Flow<Result<List<Transaction>>> {
        return walletRepository.getTransactionHistory(address)
            .map { transactions ->
                Result.success(transactions)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class GetStakingInfoUseCase @Inject constructor(
    private val walletRepository: IWalletRepository
) {
    suspend operator fun invoke(address: String): Flow<Result<VaultDepositor?>> {
        return walletRepository.getStakingInfo(address)
            .map { vaultDepositor ->
                Result.success(vaultDepositor)
            }
            .catch { exception ->
                emit(Result.failure(exception))
            }
    }
}

class StakeTokensUseCase @Inject constructor(
    private val walletRepository: IWalletRepository
) {
    suspend operator fun invoke(amount: Double): Result<String> {
        return walletRepository.stakeTokens(amount)
    }
}

class UnstakeTokensUseCase @Inject constructor(
    private val walletRepository: IWalletRepository
) {
    suspend operator fun invoke(amount: Double): Result<String> {
        return walletRepository.unstakeTokens(amount)
    }
}

class ConnectWalletUseCase @Inject constructor(
    private val walletRepository: IWalletRepository
) {
    suspend operator fun invoke(): Result<String> {
        return walletRepository.connectWallet()
    }
}

class DisconnectWalletUseCase @Inject constructor(
    private val walletRepository: IWalletRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return walletRepository.disconnectWallet()
    }
}