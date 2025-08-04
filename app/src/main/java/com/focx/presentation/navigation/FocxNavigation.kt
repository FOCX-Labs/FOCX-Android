package com.focx.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focx.presentation.ui.components.BottomNavigationBar
import com.focx.presentation.ui.screens.AddEditAddressScreen
import com.focx.presentation.ui.screens.AddEditProductScreen
import com.focx.presentation.ui.screens.AddressListScreen
import com.focx.presentation.ui.screens.EarnScreen
import com.focx.presentation.ui.screens.GovernanceScreen
import com.focx.presentation.ui.screens.ProductDetailScreen
import com.focx.presentation.ui.screens.ProductListScreen
import com.focx.presentation.ui.screens.ProfileScreen
import com.focx.presentation.ui.screens.SellScreen
import com.focx.presentation.ui.screens.SellerManagementScreen
import com.focx.presentation.ui.screens.SellerRegistrationScreen
import com.focx.presentation.ui.screens.SoldOrderDetailScreen
import com.focx.presentation.viewmodel.ProfileViewModel
import com.focx.presentation.viewmodel.AddEditProductViewModel
import com.focx.presentation.viewmodel.AddEditProductEffect
import com.focx.presentation.intent.AddEditProductIntent
import com.focx.presentation.ui.screens.OrderDetailScreen
import com.focx.presentation.ui.screens.OrderListScreen
import com.focx.presentation.viewmodel.OrderViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.focx.utils.Log
import com.focx.presentation.viewmodel.SellerManagementViewModel

// Full screen routes configuration
private val fullScreenRoutes = setOf(
    "search",
    "product_detail",
    "product_compare",
    "product_preview",
    "video_player",
    "image_viewer",
    "seller_dashboard",
    "seller_registration",
    "sold_order_detail",
    "add_product",
    "edit_product",
    "address_list",
    "add_address",
    "edit_address",
    "order_list",
    "order_detail"
)

@Composable
fun FocxNavigation(activityResultSender: ActivityResultSender) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Check if current route requires full screen display
    val isFullScreenRoute = fullScreenRoutes.any { route ->
        currentRoute?.startsWith(route) == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), bottomBar = {
            if (!isFullScreenRoute) {
                BottomNavigationBar(navController = navController)
            }
        }, contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "buy",
            modifier = if (isFullScreenRoute) Modifier.fillMaxSize() else Modifier.padding(innerPadding)
        ) {
            composable("buy") {
                ProductListScreen(
                    onProductClick = { product ->
                        navController.navigate("product_detail/${product.id}")
                    })
            }

            composable("sell") {
                SellScreen(
                    activityResultSender = activityResultSender,
                    onNavigateToSellerDashboard = {
                        navController.navigate("seller_dashboard")
                    }, onNavigateToSellerRegistration = {
                        navController.navigate("seller_registration")
                    }, onNavigateToAddProduct = {
                        navController.navigate("add_product")
                    }, onNavigateToOrderDetail = { orderId ->
                        navController.navigate("sold_order_detail/$orderId")
                    })
            }

            composable("earn") {
                EarnScreen()
            }

            composable("governance") {
                GovernanceScreen()
            }

            composable("profile") {
                ProfileScreen(
                    activityResultSender = activityResultSender,
                    onNavigateToAddresses = {
                        navController.navigate("address_list")
                    },
                    onNavigateToOrders = {
                        navController.navigate("order_list")
                    }
                )
            }

            composable(
                route = "product_detail/{productId}?isEditMode={isEditMode}",
                arguments = listOf(
                    navArgument("productId") { type = NavType.StringType },
                    navArgument("isEditMode") { 
                        type = NavType.BoolType 
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                val isEditMode = backStackEntry.arguments?.getBoolean("isEditMode") ?: false
                val orderViewModel: OrderViewModel = hiltViewModel()
                ProductDetailScreen(
                    productId = productId,
                    isEditMode = isEditMode,
                    onNavigateBack = { navController.popBackStack() },
                    onBuyProduct = { product, quantity, selectedAddress, orderNote, activityResultSender ->
                        orderViewModel.buyProduct(product, quantity.toUInt(), selectedAddress, orderNote, activityResultSender) { result ->
                            result.onSuccess {
                                navController.navigate("order_confirm/${it.id}")
                            }.onFailure {
                                // Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onEditProduct = { productId ->
                        navController.navigate("edit_product/$productId")
                    },
                    activityResultSender = activityResultSender,
                    navController = navController
                )
            }

            // Seller Registration
            composable("seller_registration") {
                SellerRegistrationScreen(
                    activityResultSender = activityResultSender,
                    onRegistrationSuccess = {
                        navController.popBackStack()
                    })
            }

            // Seller Management
            composable("seller_dashboard") {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                val sellerManagementViewModel: SellerManagementViewModel = hiltViewModel()
                val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                
                // Ensure ProfileViewModel loads data
                LaunchedEffect(Unit) {
                    profileViewModel.loadProfileData()
                }
                
                Log.d("FocxNavigation", "seller_dashboard - profileState.user?.walletAddress: ${profileState.user?.walletAddress}")
                
                SellerManagementScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }, 
                    onAddProductClick = {
                        navController.navigate("add_product")
                    }, 
                    onProductClick = { productId ->
                        navController.navigate("product_detail/$productId?isEditMode=true")
                    }, 
                    onEditProductClick = { productId ->
                        navController.navigate("edit_product/$productId")
                    },
                    merchantAddress = profileState.user?.walletAddress,
                    navController = navController,
                    viewModel = sellerManagementViewModel
                )
            }

            // Sold Order Detail
            composable("sold_order_detail/{orderId}") { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                SoldOrderDetailScreen(
                    orderId = orderId, 
                    activityResultSender = activityResultSender,
                    onBackClick = {
                        navController.popBackStack()
                    })
            }

            // Add Product
            composable("add_product") {
                val viewModel: AddEditProductViewModel = hiltViewModel()
                
                // Collect effects from ViewModel
                LaunchedEffect(Unit) {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is AddEditProductEffect.NavigateBack -> {
                                // Navigate back with refresh flag only when saving is successful
                                Log.d("FocxNavigation", "Setting refresh flag for add_product")
                                navController.previousBackStackEntry?.savedStateHandle?.set("refresh_products", true)
                                navController.popBackStack()
                            }
                            is AddEditProductEffect.ShowMessage -> {
                                // You can show a toast or snackbar here
                                // For now, we'll just log the message
                                android.util.Log.d("AddProduct", effect.message)
                            }
                        }
                    }
                }
                
                AddEditProductScreen(
                    productId = null, 
                    onBackClick = {
                        // Don't set refresh flag when user manually goes back
                        navController.popBackStack()
                    }, 
                    onSaveClick = { productData, activityResultSender ->
                        // Update form data in ViewModel first
                        viewModel.handleIntent(AddEditProductIntent.UpdateFormData(productData))
                        // Then save with ActivityResultSender
                        viewModel.handleIntent(AddEditProductIntent.SaveProduct(activityResultSender))
                        // Don't call popBackStack here, let ViewModel handle navigation
                    },
                    activityResultSender = activityResultSender,
                    viewModel = viewModel
                )
            }

            // Edit Product
            composable("edit_product/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                val viewModel: AddEditProductViewModel = hiltViewModel()
                
                // Collect effects from ViewModel
                LaunchedEffect(Unit) {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is AddEditProductEffect.NavigateBack -> {
                                // Navigate back with refresh flag only when saving is successful
                                Log.d("FocxNavigation", "Setting refresh flag for edit_product")
                                navController.previousBackStackEntry?.savedStateHandle?.set("refresh_products", true)
                                
                                // Also set refresh flag for product detail if we're coming from there
                                val previousRoute = navController.previousBackStackEntry?.destination?.route
                                if (previousRoute?.startsWith("product_detail") == true) {
                                    Log.d("FocxNavigation", "Setting refresh flag for product_detail")
                                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_product_detail", true)
                                }
                                
                                navController.popBackStack()
                            }
                            is AddEditProductEffect.ShowMessage -> {
                                // You can show a toast or snackbar here
                                // For now, we'll just log the message
                                android.util.Log.d("EditProduct", effect.message)
                            }
                        }
                    }
                }
                
                AddEditProductScreen(
                    productId = productId, 
                    onBackClick = {
                        // Don't set refresh flag when user manually goes back
                        navController.popBackStack()
                    }, 
                    onSaveClick = { productData, activityResultSender ->
                        // Update form data in ViewModel first
                        viewModel.handleIntent(AddEditProductIntent.UpdateFormData(productData))
                        // Then save with ActivityResultSender
                        viewModel.handleIntent(AddEditProductIntent.SaveProduct(activityResultSender))
                        // Don't call popBackStack here, let ViewModel handle navigation
                    },
                    activityResultSender = activityResultSender,
                    viewModel = viewModel
                )
            }

            // Address List
            composable("address_list") {
                AddressListScreen(onNavigateBack = {
                    navController.popBackStack()
                }, onNavigateToAddAddress = {
                    navController.navigate("add_address")
                }, onNavigateToEditAddress = { address ->
                    navController.navigate("edit_address/${address.id}")
                })
            }

            // Add Address
            composable("add_address") {
                AddEditAddressScreen(addressToEdit = null, onNavigateBack = {
                    navController.popBackStack()
                }, onSaveSuccess = {
                    navController.popBackStack()
                })
            }

            // Edit Address
            composable(
                "edit_address/{addressId}", arguments = listOf(navArgument("addressId") { type = NavType.StringType })
            ) { backStackEntry ->
                val addressId = backStackEntry.arguments?.getString("addressId") ?: ""

                AddEditAddressScreen(
                    addressId = addressId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }, 
                    onSaveSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            // Order List
            composable("order_list") {
                OrderListScreen(onNavigateBack = {
                    navController.popBackStack()
                }, onOrderClick = { orderId ->
                    navController.navigate("order_detail/$orderId")
                })
            }

            // Order Detail
            composable(
                "order_detail/{orderId}", arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailScreen(
                    orderId = orderId,
                    activityResultSender = activityResultSender,
                    onNavigateBack = {
                        navController.popBackStack()
                    })
            }

            // Future full screen routes can be added here
            // composable("product_compare/{productIds}") { ... }
            // composable("product_preview/{productId}") { ... }
            // composable("video_player/{videoId}") { ... }
            // composable("image_viewer/{imageUrl}") { ... }
        }
    }
}