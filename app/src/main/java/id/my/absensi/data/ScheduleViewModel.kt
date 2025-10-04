package id.my.matahati.absensi.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScheduleViewModel : ViewModel() {

    private val _schedules = MutableStateFlow<List<UserSchedule>>(emptyList())
    val schedules: StateFlow<List<UserSchedule>> = _schedules

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Load jadwal shift berdasarkan userId
     */
    fun loadSchedules(userId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = ApiClient.apiService.getUserSchedules(userId)
                // Kalau pakai Retrofit suspending fun langsung list -> bisa langsung assign
                _schedules.value = response ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.localizedMessage
                _schedules.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
