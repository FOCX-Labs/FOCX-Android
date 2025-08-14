package com.focx.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focx.domain.entity.Dispute
import com.focx.domain.entity.DisputeStatus
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.domain.entity.ProposalStatus
import com.focx.domain.entity.ProposalType
import com.focx.presentation.ui.theme.FocxTheme
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.viewmodel.GovernanceViewModel
import com.focx.utils.TimeUtils
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GovernanceScreen(
    activityResultSender: ActivityResultSender,
    viewModel: GovernanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Proposals", "Rules")

    val listState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            if (uiState.selectedTab != 0 || uiState.proposals.isEmpty()) return@derivedStateOf false

            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount

            println("Scroll check - lastVisible: $lastVisibleItem, totalItems: $totalItems, hasMore: ${uiState.hasMoreProposals}, isLoading: ${uiState.isLoadingMore}")

            // Check if we're near the bottom (within 3 items of the end)
            lastVisibleItem >= totalItems - 3 && totalItems > 0
        }
    }

    // Pull to refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    // Auto load more when scrolling to bottom
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiState.selectedTab == 0 && uiState.hasMoreProposals && !uiState.isLoadingMore) {
            println("Loading more proposals - shouldLoadMore: ${shouldLoadMore.value}, hasMore: ${uiState.hasMoreProposals}, isLoading: ${uiState.isLoadingMore}")
            viewModel.loadMoreProposals()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                state = listState,
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
                            GovernanceStatCard(
                                title = "Total Proposals",
                                value = "${uiState.stats.totalProposals}",
                                subtitle = "",
                                subtitleColor = Color(0xFFFFA726),
                                modifier = Modifier.weight(1f)
                            )
                            GovernanceStatCard(
                                title = "Voting Power",
                                value = "${uiState.stats.totalVotingPower / 1e9}",
                                subtitle = "",
                                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.showCreateProposalDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Create Proposal",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Tab Navigation
                item {
                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = uiState.selectedTab == index,
                                onClick = { viewModel.selectTab(index) },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            )
                        }
                    }
                }

                // Tab Content - Proposals
                if (uiState.selectedTab == 0) {
                    items(uiState.proposals) { proposal ->
                        ProposalCard(
                            proposal = proposal,
                            onVoteFor = { proposalId ->
                                viewModel.voteForProposal(proposalId, activityResultSender)
                            },
                            onVoteAgainst = { proposalId ->
                                viewModel.voteAgainstProposal(proposalId, activityResultSender)
                            },
                            onFinalizeProposal = { proposalId, proposerPubKey ->
                                viewModel.finalizeProposal(
                                    proposalId,
                                    proposerPubKey,
                                    activityResultSender
                                )
                            },
                            isVoting = uiState.isVoting,
                            canVote = uiState.stats.canVote
                        )
                    }
                }

                // Tab Content - Platform Rules
                if (uiState.selectedTab == 1) {
                    items(uiState.platformRules) { rule ->
                        PlatformRuleCard(rule = rule)
                    }
                }

                // Tab Content - Disputes
//                if (uiState.selectedTab == 2) {
//                    items(uiState.disputes) { dispute ->
//                        DisputeCard(dispute = dispute)
//                    }
//                }

                // Load more indicator for proposals
                if (uiState.selectedTab == 0) {
                    if (uiState.isLoadingMore || uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Pull refresh indicator
            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Error message display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(Spacing.medium)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Create Proposal Dialog
        if (uiState.showCreateProposalDialog) {
            CreateProposalDialog(
                onDismiss = { viewModel.hideCreateProposalDialog() },
                onCreateProposal = { title, description, proposalType ->
                    viewModel.createProposal(title, description, proposalType, activityResultSender)
                }
            )
        }
    }
}

@Composable
fun GovernanceStatCard(
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ), shape = RoundedCornerShape(12.dp)
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
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor
//            )
        }
    }
}

@Preview
@Composable
fun ProposalCardPreview() {
    // Create a sample proposal for preview
    val sampleProposal = Proposal(
        discriminator = 0L,
        id = 1UL,
        proposer = com.solana.publickey.SolanaPublicKey.from("11111111111111111111111111111111"),
        proposalType = com.focx.domain.entity.ProposalType.RULE_UPDATE,
        title = "Sample Proposal",
        description = "This is a sample proposal for preview purposes",
        depositAmount = 500UL,
        createdAt = System.currentTimeMillis(),
        votingStart = System.currentTimeMillis(),
        votingEnd = System.currentTimeMillis() + 86400000,
        status = com.focx.domain.entity.ProposalStatus.PENDING,
        yesVotes = 100UL,
        noVotes = 10UL,
        abstainVotes = 5UL,
        vetoVotes = 0UL,
        totalVotes = 115UL,
        executionData = null,
        executionResult = null,
        bump = 0U
    )
    ProposalCard(sampleProposal, canVote = true, onFinalizeProposal = {} as (ULong, SolanaPublicKey) -> Unit)
}

@Composable
fun ProposalCard(
    proposal: Proposal,
    onVoteFor: (ULong) -> Unit = {},
    onVoteAgainst: (ULong) -> Unit = {},
    onFinalizeProposal: (ULong, SolanaPublicKey) -> Unit,
    isVoting: Boolean = false,
    canVote: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ), shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = proposal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier, verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF4CAF50), shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${proposal.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF4CAF50), shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${proposal.proposalType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = "Vote End Time : ${TimeUtils.formatExpiryTime(proposal.votingEnd)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = proposal.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = "Voting Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${proposal.totalVotes} votes",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = {
                    if (proposal.totalVotes > 0UL) proposal.yesVotes.toLong()
                        .toFloat() / proposal.totalVotes.toLong().toFloat() else 0f
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "For: ${proposal.yesVotes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Against: ${proposal.noVotes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF5722),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

//            Row(
//                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//
//            }
            Text(
                text = "Proposed by: ${proposal.proposer.toString().take(8)}...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Security Deposit: ${proposal.depositAmount.toDouble() / 1e9} USDC",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            if (proposal.status === ProposalStatus.PENDING && canVote && System.currentTimeMillis() / 1000 < proposal.votingEnd) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    OutlinedButton(
                        onClick = { onVoteFor(proposal.id) },
                        enabled = !isVoting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFF4CAF50)
                        )
                    ) {
                        if (isVoting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF4CAF50),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Vote For")
                        }
                    }
                    OutlinedButton(
                        onClick = { onVoteAgainst(proposal.id) },
                        enabled = !isVoting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5722)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFFF5722)
                        )
                    ) {
                        if (isVoting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFFFF5722),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Vote Against")
                        }
                    }
                }
            }

            if (System.currentTimeMillis() / 1000 > proposal.votingEnd) {
                Spacer(modifier = Modifier.height(Spacing.medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    OutlinedButton(
                        onClick = { onFinalizeProposal(proposal.id, proposal.proposer) },
                        enabled = !isVoting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFF4CAF50)
                        )
                    ) {
                        if (isVoting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF4CAF50),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Finalize Proposal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformRuleCard(
    rule: PlatformRule
) {
    Card(
        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ), shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Text(
                text = rule.category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            rule.rules.forEach { ruleText ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = ruleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun DisputeCardPreview() {
    // Create a sample dispute for preview
    val sampleDispute = Dispute(
        id = "dispute_001",
        title = "Sample Dispute",
        buyer = "buyer123",
        order = "order_12345",
        amount = "299.99 USDC",
        submitted = "12/15/2024",
        status = DisputeStatus.UNDER_REVIEW,
        daysRemaining = 7,
        evidenceSummary = "Sample evidence summary",
        communityVoting = com.focx.domain.entity.CommunityVoting(buyerFavor = 15, sellerFavor = 8),
        resolution = null
    )
    DisputeCard(sampleDispute)
}

@Composable
fun DisputeCard(
    dispute: Dispute
) {
    Card(
        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ), shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (dispute.status) {
                                    DisputeStatus.UNDER_REVIEW -> Color(0xFFFFA726)
                                    DisputeStatus.VOTING -> Color(0xFF2196F3)
                                    DisputeStatus.RESOLVED -> Color(0xFF4CAF50)
                                }, shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dispute.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }

            }
            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (dispute.status) {
                        DisputeStatus.UNDER_REVIEW -> "Under Review"
                        DisputeStatus.VOTING -> "Voting"
                        DisputeStatus.RESOLVED -> "Resolved"
                    }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                if (dispute.status != DisputeStatus.RESOLVED) {
                    Text(
                        text = "${dispute.daysRemaining} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Closed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Buyer: ${dispute.buyer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Order: ${dispute.order}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Amount: ${dispute.amount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Submitted: ${dispute.submitted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = "Evidence Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = dispute.evidenceSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (dispute.communityVoting != null) {
                Spacer(modifier = Modifier.height(Spacing.small))

                Text(
                    text = "Community Voting",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Buyer Favor: ${dispute.communityVoting.buyerFavor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Seller Favor: ${dispute.communityVoting.sellerFavor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF5722),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (dispute.resolution != null) {
                Spacer(modifier = Modifier.height(Spacing.small))

                Text(
                    text = "Resolution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = dispute.resolution,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (dispute.status == DisputeStatus.VOTING) {
                Spacer(modifier = Modifier.height(Spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Button(
                        onClick = { /* Handle vote for buyer */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Vote for Buyer")
                    }
                    Button(
                        onClick = { /* Handle vote for seller */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("Vote for Seller")
                    }
                }
            }

            if (dispute.status == DisputeStatus.UNDER_REVIEW) {
                Spacer(modifier = Modifier.height(Spacing.medium))

                Button(
                    onClick = { /* Handle view details */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View Details")
                }
            }

            if (dispute.status == DisputeStatus.RESOLVED) {
                Spacer(modifier = Modifier.height(Spacing.medium))

                Button(
                    onClick = { /* Handle case closed */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = false
                ) {
                    Text("Case Closed")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProposalDialog(
    onDismiss: () -> Unit,
    onCreateProposal: (String, String, ProposalType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedProposalType by remember { mutableStateOf(ProposalType.RULE_UPDATE) }
    var expanded by remember { mutableStateOf(false) }

    val maxDescriptionLength = 800
    val descriptionLength = description.length
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    // Debug logging for state changes
    LaunchedEffect(expanded) {
        println("Dropdown expanded state changed to: $expanded")
    }

    LaunchedEffect(selectedProposalType) {
        println("Selected proposal type changed to: ${selectedProposalType.name}")
    }

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                onDismiss()
            }
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
                .padding(top = Spacing.small, bottom = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create New Proposal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { /* Focus next field */ }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = {
                    if (it.length <= maxDescriptionLength) {
                        description = it
                    }
                },
                label = { Text("Description") },
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { /* Hide keyboard */ }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                supportingText = {
                    Text(
                        text = "$descriptionLength/$maxDescriptionLength",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (descriptionLength > maxDescriptionLength * 0.9)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            // Proposal Type Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    OutlinedTextField(
                        value = selectedProposalType.name.replace("_", " "),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Proposal Type") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        supportingText = {
                            Text(
                                text = "Current: ${selectedProposalType.name.replace("_", " ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    // Dropdown options
                    if (expanded) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = Spacing.small)
                            ) {
                                ProposalType.values().forEach { proposalType ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                println("Selected proposal type: ${proposalType.name}")
                                                println("Previous selection: ${selectedProposalType.name}")
                                                selectedProposalType = proposalType
                                                expanded = false
                                                println("New selection: ${selectedProposalType.name}")
                                            }
                                            .padding(
                                                horizontal = Spacing.medium,
                                                vertical = Spacing.small
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = proposalType.name.replace("_", " "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selectedProposalType == proposalType)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (selectedProposalType == proposalType) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    if (proposalType != ProposalType.values().last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = Spacing.medium),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Invisible clickable overlay - only covers the text field area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp) // Approximate height of OutlinedTextField
                        .clickable {
                            println("Dropdown clicked, current expanded: $expanded")
                            expanded = !expanded
                        }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            onCreateProposal(title, description, selectedProposalType)
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    },
                    enabled = title.isNotBlank() && description.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Create Proposal")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Add minimal bottom padding for system navigation
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun GovernanceScreenPreview() {
    FocxTheme(darkTheme = true) {
        // Preview without ActivityResultSender for design purposes
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Governance Screen Preview")
        }
    }
}

