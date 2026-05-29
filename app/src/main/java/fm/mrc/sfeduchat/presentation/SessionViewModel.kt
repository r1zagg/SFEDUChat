package fm.mrc.sfeduchat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fm.mrc.sfeduchat.domain.model.User
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionViewModel(
    authRepository: AuthRepository,
) : ViewModel() {
    val currentUser: StateFlow<User?> = authRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
}
