package com.focx.data.networking

import com.focx.core.network.NetworkConfig
import com.solana.networking.HttpNetworkDriver
import com.solana.networking.HttpRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorHttpDriver @Inject constructor() : HttpNetworkDriver {
    override suspend fun makeHttpRequest(request: HttpRequest): String =
        HttpClient(Android) {
            // Configure timeouts using NetworkConfig settings
            install(HttpTimeout) {
                connectTimeoutMillis = NetworkConfig.CONNECTION_TIMEOUT_MS
                requestTimeoutMillis = NetworkConfig.READ_TIMEOUT_MS
                socketTimeoutMillis = NetworkConfig.WRITE_TIMEOUT_MS
            }
        }.use { client ->
            client.request(request.url) {
                method = HttpMethod.parse(request.method)
                request.properties.forEach { (k, v) ->
                    header(k, v)
                }
                setBody(request.body)
            }.bodyAsText()
        }
}