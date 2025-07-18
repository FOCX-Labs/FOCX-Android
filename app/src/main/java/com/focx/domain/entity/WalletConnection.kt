package com.focx.domain.entity

import com.solana.publickey.SolanaPublicKey

sealed class WalletConnection

object NotConnected : WalletConnection()

data class Connected(
    val publicKey: SolanaPublicKey,
    val accountLabel: String,
    val authToken: String
) : WalletConnection()