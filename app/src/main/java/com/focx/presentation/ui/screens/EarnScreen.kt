package com.focx.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focx.domain.entity.StakeActivity
import com.focx.domain.entity.Vault
import com.focx.domain.entity.VaultDepositor
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.viewmodel.EarnViewModel
import com.focx.utils.TimeUtils
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EarnScreen(
    activityResultSender: ActivityResultSender,
    viewModel: EarnViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var stakeAmount by remember { mutableStateOf("") }
    var unstakeAmount by remember { mutableStateOf("") }

    val tabs = listOf("Stake", "Unstake")

    // Load earn data when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadEarnData()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadEarnData() }
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else {
            val pullRefreshState = rememberPullRefreshState(
                refreshing = uiState.isLoading,
                onRefresh = { viewModel.loadEarnData() }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.small))
                    }

                    // Statistics Cards
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                EarnStatCard(
                                    title = "Total Value Locked",
                                    value = uiState.vault?.let {
                                        "${
                                            String.format(
                                                "%.0fK",
                                                it.totalAssets.toDouble() / 1_000_000_000.0 / 1000
                                            )
                                        }"
                                    } ?: "$0K",
                                    subtitle = "USDC",
                                    subtitleColor = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                                EarnStatCard(
                                    title = "Current APY",
                                    value = uiState.vault?.let {
                                        val apy = if (it.totalShares > 0UL) {
                                            (it.totalRewards.toDouble() / it.totalAssets.toDouble()) * 100
                                        } else 0.0
                                        "${String.format("%.1f", apy)}%"
                                    } ?: "0.0%",
                                    subtitle = "30-day average",
                                    subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                EarnStatCard(
                                    title = "Total Stakers",
                                    value = uiState.vault?.totalShares?.toString() ?: "0",
                                    subtitle = "Active participants",
                                    subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                EarnStatCard(
                                    title = "My Position",
                                    value = uiState.stakingInfo?.let { "${it.totalStaked.toDouble() / 1_000_000_000.0} USDC" }
                                        ?: "0 USDC",
                                    subtitle = "",
                                    subtitleColor = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Stake/Unstake Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.medium)
                            ) {
                                TabRow(
                                    selectedTabIndex = selectedTab,
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    tabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTab == index,
                                            onClick = { selectedTab = index },
                                            text = {
                                                Text(
                                                    text = title,
                                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.medium))

                                when (selectedTab) {
                                    0 -> StakeTab(
                                        amount = stakeAmount,
                                        onAmountChange = { stakeAmount = it },
                                        stakingInfo = uiState.stakingInfo,
                                        onStakeClick = { amount ->
                                            amount.toDoubleOrNull()?.let { doubleAmount ->
                                                if (doubleAmount > 0) {
                                                    // Convert USDC amount to smallest unit (1 USDC = 1,000,000 units)
                                                    val usdcAmount =
                                                        (doubleAmount * 1_000_000_000).toULong()
                                                    viewModel.stakeUsdc(
                                                        usdcAmount,
                                                        activityResultSender
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    1 -> UnstakeTab(
                                        amount = unstakeAmount,
                                        onAmountChange = { unstakeAmount = it },
                                        stakingInfo = uiState.stakingInfo,
                                        vault = uiState.vault,
                                        onRequestUnstakeClick = { amount ->
                                            amount.toDoubleOrNull()?.let { doubleAmount ->
                                                if (doubleAmount > 0) {
                                                    // Convert USDC amount to smallest unit (1 USDC = 1,000,000 units)
                                                    val usdcAmount =
                                                        (doubleAmount * 1_000_000_000).toULong()
                                                    viewModel.requestUnstakeUsdc(
                                                        usdcAmount,
                                                        activityResultSender
                                                    )
                                                }
                                            }
                                        },
                                        onUnstakeClick = { amount ->
                                            amount.toDoubleOrNull()?.let { doubleAmount ->
                                                // Convert USDC amount to smallest unit (1 USDC = 1,000,000 units)
                                                val usdcAmount =
                                                    (doubleAmount * 1_000_000_000).toULong()
                                                viewModel.unstakeUsdc(
                                                    usdcAmount,
                                                    activityResultSender
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Vault Information
                    item {
                        VaultInformationCard(vault = uiState.vault)
                    }

                    // Recent Activity
//            item {
//                RecentActivityCard(activities = uiState.stakeActivities)
//            }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                PullRefreshIndicator(
                    refreshing = uiState.isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        // Initialize Vault Depositor Confirmation Dialog
        if (uiState.showInitializeVaultDepositorDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissInitializeVaultDepositorDialog() },
                title = {
                    Text(
                        text = "Initialize Vault Depositor",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "You need to initialize your vault depositor account before staking. This is a one-time setup that creates your staking account on the blockchain. Would you like to proceed?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmInitializeVaultDepositor(activityResultSender) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Initialize")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissInitializeVaultDepositorDialog() }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun EarnStatCard(
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
    }
}

@Composable
fun StakeTab(
    amount: String,
    onAmountChange: (String) -> Unit,
    stakingInfo: VaultDepositor?,
    onStakeClick: (String) -> Unit
) {
    Column {
        Text(
            text = "Amount to Stake",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            TextButton(
                onClick = { onAmountChange("1000") }
            ) {
                Text("Max")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = "Balance: ${stakingInfo?.totalStaked?.let { it / 1_000_000UL } ?: 0UL} USDC",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Staked:",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${stakingInfo?.totalStaked?.let { it.toDouble() / 1_000_000_000.0 } ?: 0.0} USDC",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(Spacing.medium))

        Button(
            onClick = { onStakeClick(amount) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null
        ) {
            Text("Stake USDC")
        }
    }
}

@Composable
fun UnstakeTab(
    amount: String,
    onAmountChange: (String) -> Unit,
    stakingInfo: VaultDepositor?,
    vault: Vault?,
    onRequestUnstakeClick: (String) -> Unit,
    onUnstakeClick: (String) -> Unit
) {
    Column {
        Text(
            text = "Amount to Unstake",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            TextButton(
                onClick = { onAmountChange("500") }
            ) {
                Text("Max")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = "Staked: ${stakingInfo?.totalStaked?.let { it / 1_000_000_000UL } ?: 0UL} USDC",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Warning Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFFFA726).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(Spacing.small)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Warning",
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Column {
                    val lockupPeriodText = vault?.unstakeLockupPeriod?.let {
                        TimeUtils.formatDuration(it)
                    } ?: "14 days"
                    Text(
                        text = "${lockupPeriodText} Withdrawal Period",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFA726)
                    )
                    Text(
                        text = "Unstaked funds and accumulated rewards will be available for withdrawal after ${lockupPeriodText}. Rewards cannot be claimed separately, and must be withdrawn together with the staked amount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.medium))

        Button(
            onClick = { onRequestUnstakeClick(amount) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null
        ) {
            Text("Request Unstake")
        }

        // Show unstake request status if there's an active request
        stakingInfo?.unstakeRequest?.let { unstakeRequest ->
            if (unstakeRequest.requestTime > 0 && unstakeRequest.shares > 0U) {
                val lockupPeriod = vault?.unstakeLockupPeriod ?: 1209600L
                val isReady = TimeUtils.isUnstakeReady(unstakeRequest.requestTime, lockupPeriod)
                val expiryTime =
                    TimeUtils.getUnstakeExpiryTime(unstakeRequest.requestTime, lockupPeriod)

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isReady) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(
                            0xFFFFA726
                        ).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.small)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Status",
                                tint = if (isReady) Color(0xFF4CAF50) else Color(0xFFFFA726),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                            Text(
                                text = if (isReady) "Unstake Ready" else "Unstake Pending",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isReady) Color(0xFF4CAF50) else Color(0xFFFFA726)
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.small))

                        if (isReady) {
                            Text(
                                text = "Your unstake request is ready for withdrawal. You can now withdraw your funds and accumulated rewards.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(Spacing.small))

                            Button(
                                onClick = { onUnstakeClick(unstakeRequest.shares.toString()) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text("Withdraw Funds")
                            }
                        } else {
                            Text(
                                text = "${stakingInfo.unstakeRequest.shares} shares",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Expires at: ${TimeUtils.formatExpiryTime(expiryTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Your unstake request will be ready for withdrawal after the lockup period expires.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultInformationCard(vault: Vault?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vault Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = "Fee Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Platform Team",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${vault?.let { (it.managementFee.toDouble() / 100.0).toInt() } ?: 0}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Insurance Holders",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${vault?.let { (100.0 - it.managementFee.toDouble() / 100.0).toInt() } ?: 0}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = "Risk Coverage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Column {
                Text(
                    text = "• Merchant defaults",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Platform disputes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Regulatory penalties",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentActivityCard(
    activities: List<StakeActivity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            activities.forEach { activity ->
                ActivityItem(activity = activity)
                if (activity != activities.last()) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
            }
        }
    }
}

@Composable
fun ActivityItem(
    activity: StakeActivity
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (activity.isStake) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = activity.type,
                tint = if (activity.isStake) Color(0xFF4CAF50) else Color(0xFFFF5722),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Column {
                Text(
                    text = activity.type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = activity.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = activity.amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Note: Preview is disabled because EarnScreen requires ActivityResultSender
// which cannot be properly mocked in preview context
/*
@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun EarnScreenPreview() {
    FocxTheme(darkTheme = true) {
        EarnScreen(activityResultSender = ActivityResultSender(Activity()))
    }
}
*/