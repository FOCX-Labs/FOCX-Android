package com.focx.data.model

/**
 * Result of a Solana transaction operation
 */
data class TransactionResult(
    val isSuccess: Boolean,
    val signature: String? = null,
    val error: String? = null,
    val confirmations: Int = 0,
    val slot: Long? = null
) {
    companion object {
        fun success(signature: String, confirmations: Int = 0, slot: Long? = null) = TransactionResult(
            isSuccess = true,
            signature = signature,
            confirmations = confirmations,
            slot = slot
        )

        fun failure(error: String) = TransactionResult(
            isSuccess = false,
            error = error
        )
    }
}