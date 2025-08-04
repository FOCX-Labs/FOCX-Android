package com.focx.utils

import com.focx.domain.entity.CreateProductBase
import com.focx.domain.entity.CreateProductExtended
import com.focx.domain.entity.UpdateProduct
import com.funkatronics.kborsh.Borsh
import com.solana.serialization.AnchorInstructionSerializer

object DebugUtils {
    private val TAG = "DebugUtils"

    fun decodeCreateProductBase(str: String) {
        val bytes = str.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val obj = Borsh.decodeFromByteArray<CreateProductBase>(AnchorInstructionSerializer("create_product_base"), bytes)

        Log.d(TAG, "decodeCreateProductBase : $obj")
    }

    fun decodeCreateProductExtended(str: String) {
        val bytes = str.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val obj = Borsh.decodeFromByteArray<CreateProductExtended>(AnchorInstructionSerializer("create_product_base"), bytes)

        Log.d(TAG, "decodeCreateProductExtended : $obj")
    }

    fun decodeUpdateProduct(str: String) {
        val bytes = str.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val obj = Borsh.decodeFromByteArray<UpdateProduct>(AnchorInstructionSerializer("update_product"), bytes)

        Log.d(TAG, "decodeCreateProductExtended : $obj")
    }
}