package com.focx.di

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.focx.core.network.NetworkConfig
import com.focx.core.network.NetworkPreferences
import com.focx.data.constants.PreferencesConstants
import com.focx.data.datasource.mock.MockGovernanceDataSource
import com.focx.data.datasource.mock.MockSellerDataSource
import com.focx.data.datasource.mock.MockUserDataSource
import com.focx.data.datasource.mock.MockWalletDataSource
import com.focx.data.datasource.local.AddressLocalDataSource
import com.focx.data.datasource.local.AccountCacheDataSource
import com.focx.data.datasource.solana.SolanaMerchantDataSource
import com.focx.data.datasource.solana.SolanaOrderDataSource
import com.focx.data.datasource.solana.SolanaProductDataSource
import com.focx.data.datasource.solana.SolanaFaucetDataSource
import com.focx.data.networking.KtorHttpDriver
import com.focx.domain.repository.IGovernanceRepository
import com.focx.domain.repository.IMerchantRepository
import com.focx.domain.repository.IOrderRepository
import com.focx.domain.repository.IProductRepository
import com.focx.domain.repository.ISellerRepository
import com.focx.domain.repository.IUserRepository
import com.focx.domain.repository.IWalletRepository
import com.focx.domain.usecase.ConnectWalletUseCase
import com.focx.domain.usecase.DisconnectWalletUseCase
import com.focx.domain.usecase.GetCurrentUserUseCase
import com.focx.domain.usecase.GetCurrentWalletAddressUseCase
import com.focx.domain.usecase.GetGovernanceDataUseCase
import com.focx.domain.usecase.GetMerchantStatusUseCase
import com.focx.domain.usecase.GetOrdersBySellerUseCase
import com.focx.domain.usecase.GetProductByIdUseCase
import com.focx.domain.usecase.GetProductsUseCase
import com.focx.domain.usecase.GetSellerStatsUseCase
import com.focx.domain.usecase.GetStakingInfoUseCase
import com.focx.domain.usecase.GetUserAddressesUseCase
import com.focx.domain.usecase.GetUserProfileUseCase
import com.focx.domain.usecase.GetWalletBalanceUseCase
import com.focx.domain.usecase.LoginWithWalletUseCase
import com.focx.domain.usecase.RecentBlockhashUseCase
import com.focx.domain.usecase.RegisterMerchantUseCase
import com.focx.domain.usecase.RequestUsdcFaucetUseCase
import com.focx.domain.usecase.SaveProductUseCase
import com.focx.domain.usecase.SearchProductsUseCase
import com.focx.domain.usecase.SolanaAccountBalanceUseCase
import com.focx.domain.usecase.SolanaTokenBalanceUseCase
import com.focx.domain.usecase.SolanaWalletConnectUseCase
import com.focx.domain.usecase.SolanaWalletPersistenceUseCase
import com.focx.domain.usecase.UpdateProductUseCase
import com.focx.domain.usecase.VoteOnProposalUseCase
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient
import com.solana.rpc.SolanaRpcClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideProductRepository(
        @ApplicationContext context: Context,
        walletAdapter: MobileWalletAdapter,
        recentBlockhashUseCase: RecentBlockhashUseCase,
        solanaRpcClient: SolanaRpcClient
    ): IProductRepository {
        return SolanaProductDataSource(context, walletAdapter, recentBlockhashUseCase, solanaRpcClient)
    }

    @Provides
    fun provideGetProductsUseCase(
        productRepository: IProductRepository
    ): GetProductsUseCase {
        return GetProductsUseCase(productRepository)
    }

    @Provides
    fun provideGetProductByIdUseCase(
        productRepository: IProductRepository
    ): GetProductByIdUseCase {
        return GetProductByIdUseCase(productRepository)
    }

    @Provides
    fun provideSearchProductsUseCase(
        productRepository: IProductRepository
    ): SearchProductsUseCase {
        return SearchProductsUseCase(productRepository)
    }

    @Provides
    fun provideSaveProductUseCase(
        productRepository: IProductRepository
    ): SaveProductUseCase {
        return SaveProductUseCase(productRepository)
    }

    @Provides
    fun provideUpdateProductUseCase(
        productRepository: IProductRepository
    ): UpdateProductUseCase {
        return UpdateProductUseCase(productRepository)
    }

    @Provides
    @Singleton
    fun provideOrderRepository(
        walletAdapterClient: MobileWalletAdapter,
        recentBlockhashUseCase: RecentBlockhashUseCase,
        solanaRpcClient: SolanaRpcClient
    ): IOrderRepository {
        return SolanaOrderDataSource(walletAdapterClient, recentBlockhashUseCase, solanaRpcClient)
    }

    @Provides
    fun provideGetOrdersBySellerUseCase(
        orderRepository: IOrderRepository
    ): GetOrdersBySellerUseCase {
        return GetOrdersBySellerUseCase(orderRepository)
    }

    @Provides
    fun provideGetOrderByIdUseCase(
        orderRepository: IOrderRepository
    ): com.focx.domain.usecase.GetOrderByIdUseCase {
        return com.focx.domain.usecase.GetOrderByIdUseCase(orderRepository)
    }

    @Provides
    fun provideCreateOrderUseCase(
        orderRepository: IOrderRepository
    ): com.focx.domain.usecase.CreateOrderUseCase {
        return com.focx.domain.usecase.CreateOrderUseCase(orderRepository)
    }

    @Provides
    @Singleton
    fun provideSellerRepository(): ISellerRepository {
        return MockSellerDataSource()
    }

    @Provides
    fun provideGetSellerStatsUseCase(
        sellerRepository: ISellerRepository
    ): GetSellerStatsUseCase {
        return GetSellerStatsUseCase(sellerRepository)
    }

    @Provides
    @Singleton
    fun provideGovernanceRepository(): IGovernanceRepository {
        return MockGovernanceDataSource()
    }

    @Provides
    fun provideGetGovernanceDataUseCase(
        governanceRepository: IGovernanceRepository
    ): GetGovernanceDataUseCase {
        return GetGovernanceDataUseCase(governanceRepository)
    }

    @Provides
    fun provideVoteOnProposalUseCase(
        governanceRepository: IGovernanceRepository
    ): VoteOnProposalUseCase {
        return VoteOnProposalUseCase(governanceRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepository(): IUserRepository {
        return MockUserDataSource()
    }

    @Provides
    @Singleton
    fun provideWalletRepository(): IWalletRepository {
        return MockWalletDataSource()
    }

    @Provides
    fun provideGetCurrentUserUseCase(
        userRepository: IUserRepository
    ): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(userRepository)
    }

    @Provides
    fun provideGetUserProfileUseCase(
        userRepository: IUserRepository
    ): GetUserProfileUseCase {
        return GetUserProfileUseCase(userRepository)
    }

    @Provides
    fun provideGetUserAddressesUseCase(
        userRepository: IUserRepository
    ): GetUserAddressesUseCase {
        return GetUserAddressesUseCase(userRepository)
    }

    @Provides
    fun provideGetWalletBalanceUseCase(
        walletRepository: IWalletRepository
    ): GetWalletBalanceUseCase {
        return GetWalletBalanceUseCase(walletRepository)
    }

    @Provides
    fun provideGetStakingInfoUseCase(
        walletRepository: IWalletRepository
    ): GetStakingInfoUseCase {
        return GetStakingInfoUseCase(walletRepository)
    }

    @Provides
    fun provideConnectWalletUseCase(
        walletRepository: IWalletRepository
    ): ConnectWalletUseCase {
        return ConnectWalletUseCase(walletRepository)
    }

    @Provides
    fun provideDisconnectWalletUseCase(
        walletRepository: IWalletRepository
    ): DisconnectWalletUseCase {
        return DisconnectWalletUseCase(walletRepository)
    }

    @Provides
    fun provideLoginWithWalletUseCase(
        userRepository: IUserRepository
    ): LoginWithWalletUseCase {
        return LoginWithWalletUseCase(userRepository)
    }

    // Solana Wallet related providers
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PreferencesConstants.WALLET_PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideNetworkPreferences(@ApplicationContext context: Context): NetworkPreferences {
        return NetworkPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.Gson()
    }
    
    @Provides
    @Singleton
    fun provideAddressLocalDataSource(
        @ApplicationContext context: Context,
        gson: com.google.gson.Gson
    ): AddressLocalDataSource {
        return AddressLocalDataSource(context, gson)
    }

    @Provides
    @Singleton
    fun provideAccountCacheDataSource(
        @ApplicationContext context: Context
    ): AccountCacheDataSource {
        return AccountCacheDataSource(context)
    }

    @Provides
    @Singleton
    fun provideMobileWalletAdapter(): MobileWalletAdapter {
        val solanaUri = Uri.parse("https://solana.com")
        val iconUri = Uri.parse("favicon.ico")
        val identityName = "Focx"

        return MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = solanaUri,
                iconUri = iconUri,
                identityName = identityName
            )
        )
    }

    @Provides
    @Singleton
    fun provideSolanaWalletPersistenceUseCase(
        sharedPreferences: SharedPreferences
    ): SolanaWalletPersistenceUseCase {
        return SolanaWalletPersistenceUseCase(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideSolanaWalletConnectUseCase(
        walletAdapter: MobileWalletAdapter,
        persistenceUseCase: SolanaWalletPersistenceUseCase,
        loginWithWalletUseCase: LoginWithWalletUseCase
    ): SolanaWalletConnectUseCase {
        return SolanaWalletConnectUseCase(walletAdapter, persistenceUseCase, loginWithWalletUseCase)
    }

    @Provides
    @Singleton
    fun provideKtorHttpDriver(): KtorHttpDriver {
        return KtorHttpDriver()
    }

    @Provides
    @Singleton
    fun provideSolanaRpcClient(
        ktorHttpDriver: KtorHttpDriver
    ): SolanaRpcClient {
        return SolanaRpcClient(NetworkConfig.getRpcUrl(), ktorHttpDriver)
    }

    @Provides
    @Singleton
    fun provideSolanaAccountBalanceUseCase(
        solanaRpcClient: SolanaRpcClient
    ): SolanaAccountBalanceUseCase {
        return SolanaAccountBalanceUseCase(solanaRpcClient)
    }

    @Provides
    @Singleton
    fun provideSolanaTokenBalanceUseCase(
        solanaRpcClient: SolanaRpcClient
    ): SolanaTokenBalanceUseCase {
        return SolanaTokenBalanceUseCase(solanaRpcClient)
    }

    @Provides
    @Singleton
    fun provideRecentBlockhashUseCase(
        solanaRpcClient: SolanaRpcClient
    ): RecentBlockhashUseCase {
        return RecentBlockhashUseCase(solanaRpcClient)
    }

    // Solana Wallet Service

    // Merchant related providers
    @Provides
    @Singleton
    fun provideMerchantRepository(
        @ApplicationContext context: Context,
        walletAdapter: MobileWalletAdapter,
        recentBlockhashUseCase: RecentBlockhashUseCase,
        solanaRpcClient: SolanaRpcClient
    ): IMerchantRepository {
        return SolanaMerchantDataSource(context, walletAdapter, recentBlockhashUseCase, solanaRpcClient)
    }

    @Provides
    fun provideRegisterMerchantUseCase(
        merchantRepository: IMerchantRepository
    ): RegisterMerchantUseCase {
        return RegisterMerchantUseCase(merchantRepository)
    }

    @Provides
    fun provideGetMerchantStatusUseCase(
        merchantRepository: IMerchantRepository
    ): GetMerchantStatusUseCase {
        return GetMerchantStatusUseCase(merchantRepository)
    }

    @Provides
    @Singleton
    fun provideGetCurrentWalletAddressUseCase(
        solanaWalletConnectUseCase: SolanaWalletConnectUseCase
    ): GetCurrentWalletAddressUseCase {
        return GetCurrentWalletAddressUseCase(solanaWalletConnectUseCase)
    }

    @Provides
    @Singleton
    fun provideSolanaFaucetDataSource(
        @ApplicationContext context: Context,
        walletAdapter: MobileWalletAdapter,
        recentBlockhashUseCase: RecentBlockhashUseCase,
        solanaRpcClient: SolanaRpcClient
    ): SolanaFaucetDataSource {
        return SolanaFaucetDataSource(context, walletAdapter, recentBlockhashUseCase, solanaRpcClient)
    }

    @Provides
    @Singleton
    fun provideRequestUsdcFaucetUseCase(
        solanaFaucetDataSource: SolanaFaucetDataSource
    ): RequestUsdcFaucetUseCase {
        return RequestUsdcFaucetUseCase(solanaFaucetDataSource)
    }
}