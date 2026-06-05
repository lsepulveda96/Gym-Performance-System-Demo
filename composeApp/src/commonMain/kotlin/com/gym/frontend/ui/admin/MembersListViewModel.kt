package com.gym.frontend.ui.admin

import com.gym.shared.domain.GymPlan
import com.gym.shared.domain.Member
import com.gym.shared.domain.MemberRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class MemberListItemUiModel(
    val member: Member,
    val joinDateStr: String,
    val expirationDateStr: String
)

sealed interface MembersListUiState {
    data object Loading : MembersListUiState
    data class Success(
        val members: List<MemberListItemUiModel>,
        val plans: List<GymPlan>
    ) : MembersListUiState
    data class Error(val message: String) : MembersListUiState
}

sealed interface MembersListActionState {
    data object Idle : MembersListActionState
    data object Saving : MembersListActionState
    data object SaveSuccess : MembersListActionState
    data class SaveError(val message: String) : MembersListActionState
}

class MembersListViewModel(
    private val repository: MembersRepository
) {
    private val _uiState = MutableStateFlow<MembersListUiState>(MembersListUiState.Loading)
    val uiState: StateFlow<MembersListUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<MembersListActionState>(MembersListActionState.Idle)
    val actionState: StateFlow<MembersListActionState> = _actionState.asStateFlow()

    private var allMembers: List<Member> = emptyList()
    private var allPlans: List<GymPlan> = emptyList()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSortColumn = MutableStateFlow("JOIN_DATE")
    val currentSortColumn: StateFlow<String> = _currentSortColumn.asStateFlow()

    private val _sortAscending = MutableStateFlow(false)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    fun loadData(scope: CoroutineScope) {
        _uiState.value = MembersListUiState.Loading
        scope.launch {
            repository.getMembers()
                .onSuccess { members ->
                    allMembers = members
                    allPlans = repository.getPlans().getOrElse { emptyList() }
                    updateUiState()
                }
                .onFailure { e ->
                    _uiState.value = MembersListUiState.Error(e.message ?: "Failed to load members")
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateUiState()
    }

    fun toggleSort(column: String) {
        if (_currentSortColumn.value == column) {
            _sortAscending.value = !_sortAscending.value
        } else {
            _currentSortColumn.value = column
            _sortAscending.value = true
        }
        updateUiState()
    }

    fun setStatusFilter(filter: String?) {
        _statusFilter.value = filter
        updateUiState()
    }

    private fun updateUiState() {
        val query = _searchQuery.value
        val filter = _statusFilter.value
        val sortCol = _currentSortColumn.value
        val asc = _sortAscending.value

        val filtered = allMembers.filter {
            (filter == null || it.status.uppercase() == filter) &&
            (it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true))
        }.sortedWith { a, b ->
            val factor = if (asc) 1 else -1
            when (sortCol) {
                "NAME" -> factor * a.name.compareTo(b.name)
                "STATUS" -> factor * a.status.compareTo(b.status)
                "PLAN" -> factor * (a.currentPlan ?: "").compareTo(b.currentPlan ?: "")
                else -> factor * a.joinDate.compareTo(b.joinDate)
            }
        }

        val uiModels = filtered.map { member ->
            val joinDateStr = try {
                val local = member.joinDate.toLocalDateTime(TimeZone.currentSystemDefault())
                "${local.month.name.take(3)} ${local.dayOfMonth}, ${local.year}"
            } catch (e: Exception) { "Jan 12, 2024" }
            
            val expDateStr = member.expirationDate?.let {
                val local = it.toLocalDateTime(TimeZone.currentSystemDefault())
                "${local.dayOfMonth}/${local.monthNumber}/${local.year}"
            } ?: "No payment"

            MemberListItemUiModel(member, joinDateStr, expDateStr)
        }

        _uiState.value = MembersListUiState.Success(uiModels, allPlans)
    }

    fun createMember(request: MemberRequest, scope: CoroutineScope) {
        _actionState.value = MembersListActionState.Saving
        scope.launch {
            repository.createMember(request)
                .onSuccess {
                    _actionState.value = MembersListActionState.SaveSuccess
                    loadData(scope)
                }
                .onFailure { e ->
                    _actionState.value = MembersListActionState.SaveError(e.message ?: "Failed to create member")
                }
        }
    }

    fun updateMember(id: String, request: MemberRequest, scope: CoroutineScope) {
        _actionState.value = MembersListActionState.Saving
        scope.launch {
            repository.updateMember(id, request)
                .onSuccess {
                    _actionState.value = MembersListActionState.SaveSuccess
                    loadData(scope)
                }
                .onFailure { e ->
                    _actionState.value = MembersListActionState.SaveError(e.message ?: "Failed to update member")
                }
        }
    }

    fun resetActionState() {
        _actionState.value = MembersListActionState.Idle
    }
}
