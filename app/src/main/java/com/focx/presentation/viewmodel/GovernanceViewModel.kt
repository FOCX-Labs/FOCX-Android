package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.entity.Dispute
import com.focx.domain.entity.GovernanceStats
import com.focx.domain.entity.PlatformRule
import com.focx.domain.entity.Proposal
import com.focx.domain.entity.VoteType
import com.focx.domain.usecase.GetGovernanceDataUseCase
import com.focx.domain.usecase.VoteOnProposalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GovernanceUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val stats: GovernanceStats = GovernanceStats(
        activeProposals = 12,
        totalProposals = 50,
        totalVotes = 2456,
        passRate = 94.2,
        totalVotingPower = 1250.0,
        participationRate = 78.5
    ),
    val proposals: List<Proposal> = emptyList(),
    val platformRules: List<PlatformRule> = emptyList(),
    val disputes: List<Dispute> = emptyList(),
    val selectedTab: Int = 0
)

@HiltViewModel
class GovernanceViewModel @Inject constructor(
    private val getGovernanceDataUseCase: GetGovernanceDataUseCase,
    private val voteOnProposalUseCase: VoteOnProposalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GovernanceUiState())
    val uiState: StateFlow<GovernanceUiState> = _uiState.asStateFlow()

    init {
        loadGovernanceData()
    }

    private fun loadGovernanceData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load proposals from repository
                getGovernanceDataUseCase.getActiveProposals()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                    .collect { proposals ->
                        _uiState.value = _uiState.value.copy(
                            proposals = proposals
                        )
                    }

                // Load governance stats
                getGovernanceDataUseCase.getGovernanceStats()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                    .collect { stats ->
                        _uiState.value = _uiState.value.copy(
                            stats = stats
                        )
                    }

                // Load platform rules from repository
                getGovernanceDataUseCase.getPlatformRules()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                    .collect { platformRules ->
                        _uiState.value = _uiState.value.copy(
                            platformRules = platformRules
                        )
                    }

                // Load disputes from repository
                getGovernanceDataUseCase.getDisputes()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                    .collect { disputes ->
                        _uiState.value = _uiState.value.copy(
                            disputes = disputes,
                            isLoading = false
                        )
                    }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load governance data: ${e.message}"
                )
            }
        }
    }


    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
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


    fun voteOnProposal(proposalId: String, voteType: VoteType) {
        viewModelScope.launch {
            try {
                voteOnProposalUseCase.execute(proposalId, voteType)
                // Refresh data after voting
                refreshData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to vote: ${e.message}"
                )
            }
        }
    }

    fun refreshData() {
        loadGovernanceData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}