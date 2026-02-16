package com.opencode.sshterminal.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.sshterminal.data.ConnectionProfile
import com.opencode.sshterminal.data.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionListViewModel @Inject constructor(
    private val repository: ConnectionRepository
) : ViewModel() {

    val profiles: StateFlow<List<ConnectionProfile>> = repository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(profile: ConnectionProfile) {
        viewModelScope.launch { repository.save(profile) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
