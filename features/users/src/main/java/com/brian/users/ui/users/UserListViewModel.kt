package com.brian.users.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.brian.users.mapper.UserUiMapper
import com.githubbrowser.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userUiMapper: UserUiMapper,
) : ViewModel() {

    val usersPaging: StateFlow<PagingData<UserUiState>> = userRepository.getUsers()
        .map { pagingData -> pagingData.map { userUiMapper.toUiState(it) } }
        .cachedIn(viewModelScope)
        .stateIn(
            viewModelScope, started = SharingStarted.WhileSubscribed(5000),
            PagingData.empty()
        )
}

data class UserUiState(
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String,
)