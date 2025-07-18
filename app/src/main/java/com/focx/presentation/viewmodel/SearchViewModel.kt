package com.focx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focx.domain.usecase.SearchProductsUseCase
import com.focx.presentation.intent.SearchIntent
import com.focx.presentation.state.SearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchProductsUseCase: SearchProductsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeSearchQuery()
    }

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateSearchQuery -> {
                _state.value = _state.value.copy(query = intent.query)
                _searchQuery.value = intent.query
            }

            is SearchIntent.SearchProducts -> searchProducts(intent.query)
            is SearchIntent.ClearSearch -> {
                _state.value = SearchState()
                _searchQuery.value = ""
            }
        }
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .collect { query ->
                    if (query.length >= 2) {
                        searchProducts(query)
                    }
                }
        }
    }

    private fun searchProducts(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _state.value = _state.value.copy(products = emptyList(), isLoading = false)
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true)

            searchProductsUseCase(query, 1, 20) // TODO: Implement pagination
                .collect { result ->
                    result.fold(
                        onSuccess = { products ->
                            _state.value = _state.value.copy(
                                products = products,
                                isLoading = false
                            )
                        },
                        onFailure = { exception ->
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Search failed"
                            )
                        }
                    )
                }
        }
    }
}