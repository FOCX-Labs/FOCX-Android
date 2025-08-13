package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Dispute
import com.focx.domain.entity.GovernanceStats
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.domain.entity.ProposalCategory
import com.focx.domain.entity.VoteType
import com.focx.domain.usecase.CreateProposalUseCase
import com.focx.domain.usecase.GetGovernanceDataUseCase
import com.focx.domain.usecase.VoteOnProposalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import javax.inject.Inject

data class GovernanceUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val stats: GovernanceStats = GovernanceStats(
        activeProposals = 0UL,
        totalProposals = 0,
        totalVotes = 0,
        passRate = 0.0,
        totalVotingPower = 0.0,
        participationRate = 0.0
    ),
    val proposals: List<Proposal> = emptyList(),
    val platformRules: List<PlatformRule> = emptyList(),
    val disputes: List<Dispute> = emptyList(),
    val selectedTab: Int = 0,
    val currentPage: Int = 1,
    val hasMoreProposals: Boolean = true
)

@HiltViewModel
class GovernanceViewModel @Inject constructor(
    private val getGovernanceDataUseCase: GetGovernanceDataUseCase,
    private val voteOnProposalUseCase: VoteOnProposalUseCase,
    private val createProposalUseCase: CreateProposalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GovernanceUiState())
    val uiState: StateFlow<GovernanceUiState> = _uiState.asStateFlow()

    init {
        loadGovernanceData()
    }

    private fun loadGovernanceData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Load all governance data concurrently
                val statsFlow = getGovernanceDataUseCase.getGovernanceStats()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load stats: ${e.message}"
                        )
                    }

                val proposalsFlow = getGovernanceDataUseCase.getProposals(1, 10)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load proposals: ${e.message}"
                        )
                    }

                val rulesFlow = getGovernanceDataUseCase.getPlatformRules()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load rules: ${e.message}"
                        )
                    }

                val disputesFlow = getGovernanceDataUseCase.getDisputes()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load disputes: ${e.message}"
                        )
                    }

                // Collect all flows and update UI state
                statsFlow.collect { stats ->
                    _uiState.value = _uiState.value.copy(stats = stats)
                }

                proposalsFlow.collect { proposals ->
                    // For DESC pagination, we need to check if there are more proposals beyond the first page
                    // If we got a full page (10 items), there might be more
                    val hasMore = proposals.size >= 10
                    println("Initial proposals loaded: ${proposals.size}, hasMore: $hasMore")
                    println("Proposals: ${proposals.map { it.title }}")
                    _uiState.value = _uiState.value.copy(
                        proposals = proposals,
                        currentPage = 1,
                        hasMoreProposals = hasMore
                    )
                }

                rulesFlow.collect { platformRules ->
                    _uiState.value = _uiState.value.copy(platformRules = platformRules)
                }

                disputesFlow.collect { disputes ->
                    _uiState.value = _uiState.value.copy(
                        disputes = disputes,
                        isLoading = false,
                        isRefreshing = false
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "Failed to load governance data: ${e.message}"
                )
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
        // Only reset pagination if proposals list is empty
        if (tabIndex == 0 && _uiState.value.proposals.isEmpty()) {
            resetProposalPagination()
        }
    }

    fun voteForDispute(disputeId: String, favorBuyer: Boolean) {
        viewModelScope.launch {
            try {
                // Use the repository to vote on dispute
                val result = getGovernanceDataUseCase.voteOnDispute(disputeId, favorBuyer)

                if (result.isSuccess) {
                    // Refresh disputes data after successful vote
                    getGovernanceDataUseCase.getDisputes()
                        .catch { e ->
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to refresh disputes: ${e.message}"
                            )
                        }
                        .collect { disputes ->
                            _uiState.value = _uiState.value.copy(disputes = disputes)
                        }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to vote: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to vote: ${e.message}"
                )
            }
        }
    }

    fun voteOnProposal(proposalId: String, voteType: VoteType, voterPubKey: SolanaPublicKey, activityResultSender: ActivityResultSender) {
        viewModelScope.launch {
            try {
                val result = voteOnProposalUseCase.execute(proposalId, voteType, voterPubKey, activityResultSender)
                if (result.isSuccess) {
                    // Refresh data after voting
                    refreshData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to vote: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to vote: ${e.message}"
                )
            }
        }
    }

    fun createProposal(title: String, description: String, category: ProposalCategory, proposerPubKey: SolanaPublicKey, activityResultSender: ActivityResultSender) {
        viewModelScope.launch {
            try {
                val result = createProposalUseCase.execute(title, description, category, proposerPubKey, activityResultSender)
                if (result.isSuccess) {
                    // Refresh data after creating proposal
                    refreshData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to create proposal: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create proposal: ${e.message}"
                )
            }
        }
    }

    fun refreshData() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        resetProposalPagination()
        loadGovernanceData()
    }

    fun refresh() {
        refreshData()
    }

    private fun resetProposalPagination() {
        _uiState.value = _uiState.value.copy(
            proposals = emptyList(),
            currentPage = 1,
            hasMoreProposals = true
        )
    }

    fun loadMoreProposals() {
        println("loadMoreProposals called - isLoadingMore: ${_uiState.value.isLoadingMore}, hasMore: ${_uiState.value.hasMoreProposals}")
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreProposals) {
            println("loadMoreProposals early return")
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
                
                val nextPage = _uiState.value.currentPage + 1
                val pageSize = 10
                
                getGovernanceDataUseCase.getProposals(nextPage, pageSize)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load more proposals: ${e.message}",
                            isLoadingMore = false
                        )
                    }
                    .collect { newProposals ->
                        println("Load more result: ${newProposals.size} proposals")
                        if (newProposals.isEmpty()) {
                            // For DESC pagination, empty result means we've reached the end
                            _uiState.value = _uiState.value.copy(
                                hasMoreProposals = false,
                                isLoadingMore = false
                            )
                        } else {
                            // For DESC pagination, if we got less than pageSize, we're at the end
                            val hasMore = newProposals.size >= pageSize
                            _uiState.value = _uiState.value.copy(
                                proposals = _uiState.value.proposals + newProposals,
                                currentPage = nextPage,
                                hasMoreProposals = hasMore,
                                isLoadingMore = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = "Failed to load more proposals: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}