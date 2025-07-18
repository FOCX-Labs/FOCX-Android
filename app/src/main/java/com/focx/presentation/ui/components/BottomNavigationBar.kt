package com.focx.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focx.presentation.ui.theme.FocxTheme
import com.focx.presentation.ui.theme.Primary

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = "buy",
        icon = Icons.Default.ShoppingCart,
        label = "Buy"
    ),
    BottomNavItem(
        route = "sell",
        icon = Icons.Default.Store,
        label = "Sell"
    ),
    BottomNavItem(
        route = "earn",
        icon = Icons.Default.TrendingUp,
        label = "Earn"
    ),
    BottomNavItem(
        route = "governance",
        icon = Icons.Default.Gavel,
        label = "Govern"
    ),
    BottomNavItem(
        route = "profile",
        icon = Icons.Default.AccountCircle,
        label = "Profile"
    )
)

@Composable
fun BottomNavigationBar(
    navController: NavController
) {
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Color.Black.copy(alpha = 0.9f),
        shadowElevation = 8.dp,
//        border = BorderStroke(
//            width = 0.5.dp,
//            color = Color.White.copy(alpha = 0.2f)
//        )
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 32.dp else 28.dp)
                                .background(
                                    color = if (isSelected) Primary.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(if (isSelected) 20.dp else 18.dp),
                                tint = if (isSelected) Primary else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Primary else Color.White.copy(alpha = 0.8f)
                        )
                    },
                    selected = isSelected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        unselectedTextColor = Color.White.copy(alpha = 0.8f)
                    ),
                    onClick = {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun BottomNavigationBarPreview() {
    FocxTheme { BottomNavigationBar(rememberNavController()) }
}