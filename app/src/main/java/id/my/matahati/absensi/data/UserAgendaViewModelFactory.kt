package id.my.matahati.absensi.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.my.matahati.absensi.data.UserAgendaRepository

class UserAgendaViewModelFactory(
    private val repo: UserAgendaRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserAgendaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserAgendaViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
