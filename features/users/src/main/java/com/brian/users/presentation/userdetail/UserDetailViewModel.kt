package com.brian.users.presentation.userdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brian.users.presentation.mapper.UserUiMapper
import com.brian.users.utils.NAV_ARG_LOGIN_NAME
import com.brian.users.utils.safe
import com.brian.users.domain.repository.UserRepository
import com.brian.users.domain.usecase.GetUserDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val getUserDetailUseCase: GetUserDetailUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val userUiMapper: UserUiMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserDetailUiState>(UserDetailUiState.Loading)
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    private val loginName
        get() = savedStateHandle.get<String>(NAV_ARG_LOGIN_NAME).safe()

    init {
        getUserDetail()
    }

    fun getUserDetail() {
        _uiState.value = UserDetailUiState.Loading
        viewModelScope.launch {
            getUserDetailUseCase(loginName)
                .onStart {
                    _uiState.value = UserDetailUiState.Loading
                }
                .collect { result ->
                    result.onSuccess { userDetail ->
                        _uiState.value =
                            UserDetailUiState.Success(userUiMapper.toUserDetailItemUi(userDetail))
                    }
                    result.onFailure {
                        _uiState.value = UserDetailUiState.Error(it.message)
                    }
                }
        }
    }
}

sealed interface UserDetailUiState {
    data class Success(val userWithDetail: UserDetailItemUi) : UserDetailUiState
    data class Error(val error: String? = null) : UserDetailUiState
    object Loading : UserDetailUiState

}

data class UserDetailItemUi(
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val location: String? = null,
    val followers: Int,
    val following: Int,
)
