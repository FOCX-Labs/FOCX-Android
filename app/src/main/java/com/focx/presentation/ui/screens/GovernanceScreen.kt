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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focx.data.datasource.mock.MockGovernanceDataSource
import com.focx.domain.entity.Dispute
import com.focx.domain.entity.DisputeStatus
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.presentation.ui.theme.FocxTheme
import com.focx.presentation.ui.theme.Spacing
import com.focx.presentation.viewmodel.GovernanceViewModel

@Composable
fun GovernanceScreen(
    viewModel: GovernanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Proposals", "Rules", "Disputes")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium), modifier = Modifier.fillMaxWidth()
                    ) {
                        GovernanceStatCard(
                            title = "Active Proposals",
                            value = "12",
                            subtitle = "3 ending soon",
                            subtitleColor = Color(0xFFFFA726),
                            modifier = Modifier.weight(1f)
                        )
                        GovernanceStatCard(
                            title = "Total Voters",
                            value = "2,456",
                            subtitle = "+8% this month",
                            subtitleColor = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium), modifier = Modifier.fillMaxWidth()
                    ) {
                        GovernanceStatCard(
                            title = "Voting Power",
                            value = "1,250",
                            subtitle = "Based on stake",
                            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        GovernanceStatCard(
                            title = "Success Rate",
                            value = "94.2%",
                            subtitle = "Proposals passed",
                            subtitleColor = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                    }
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

            // Tab Content
            when (uiState.selectedTab) {
                0 -> {
                    // Create Proposal Button
                    item {
                        Button(
                            onClick = { /* Handle create proposal */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Create Proposal", modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Proposals List
                    items(uiState.proposals) { proposal ->
                        ProposalCard(proposal = proposal)
                    }
                }

                1 -> {
                    // Platform Rules
                    items(uiState.platformRules) { rule ->
                        PlatformRuleCard(rule = rule)
                    }
                }

                2 -> {
                    // Disputes
                    items(uiState.disputes) { dispute ->
                        DisputeCard(dispute = dispute)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun GovernanceStatCard(
    title: String, value: String, subtitle: String, subtitleColor: Color, modifier: Modifier = Modifier
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor
            )
        }
    }
}

@Preview
@Composable
fun ProposalCardPreview() {
    ProposalCard(MockGovernanceDataSource.mockGovernance[0])
}

@Composable
fun ProposalCard(
    proposal: Proposal
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
                        text = "Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "${
                        ((proposal.votingEndTime - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(
                            0
                        )
                    } days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = proposal.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = "Voting Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${proposal.totalVotes} votes", style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { if (proposal.totalVotes > 0) proposal.votesFor.toFloat() / proposal.totalVotes.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "For: ${proposal.votesFor}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Against: ${proposal.votesAgainst}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF5722),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Proposed by: ${proposal.proposerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Security Deposit: ${proposal.securityDeposit} USDC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                OutlinedButton(
                    onClick = { /* Handle vote for */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color(0xFF4CAF50)
                    )
                ) {
                    Text("Vote For")
                }
                OutlinedButton(
                    onClick = { /* Handle vote against */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5722)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color(0xFFFF5722)
                    )
                ) {
                    Text("Vote Against")
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
                text = rule.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
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
    DisputeCard(MockGovernanceDataSource.mockDisputes[0])
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
                text = "Evidence Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
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
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
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
                    text = "Resolution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
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
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.small)
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

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
fun GovernanceScreenPreview() {
    FocxTheme(darkTheme = true) {
        GovernanceScreen()
    }
}