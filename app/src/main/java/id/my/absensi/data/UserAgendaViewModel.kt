package id.my.matahati.absensi.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.matahati.absensi.data.UserAgendaRepository
import id.my.matahati.absensi.data.UserAgenda
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

class UserAgendaViewModel(
    private val repo: UserAgendaRepository
) : ViewModel() {

    // observe dari Room
    val agendas: StateFlow<List<UserAgenda>> =
        repo.getLocalAgenda()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun loadAgenda(month: String) {
        viewModelScope.launch {
            repo.syncAgenda(month)
        }
    }
}