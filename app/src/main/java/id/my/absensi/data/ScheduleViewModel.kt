package id.my.matahati.absensi.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.matahati.absensi.MyApp
import id.my.matahati.absensi.utils.NetworkUtils
import id.my.matahati.absensi.worker.enqueueScheduleSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    // 🔹 Data untuk CardShift & Kalender
    private val _schedules = MutableStateFlow<List<UserSchedule>>(emptyList())
    val schedules: StateFlow<List<UserSchedule>> = _schedules

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val dao = MyApp.db.userScheduleDao()
    private val context = getApplication<Application>().applicationContext

    /**
     * 🔹 Load jadwal (API → DB → UI)
     * Dipakai oleh:
     * - CardShift
     * - Halaman Jadwal
     * - Kalender
     */
    fun loadSchedules(userId: Int) {
        enqueueScheduleSyncWorker(context, userId)

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_DATE

            // Ambil range ± 1 minggu dari minggu ini
            val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
            val startDate = startOfWeek.minusWeeks(1).format(formatter)
            val endDate = startOfWeek.plusWeeks(2).minusDays(1).format(formatter)

            // ===== OFFLINE FIRST =====
            val localData = withContext(Dispatchers.IO) {
                dao.getSchedulesInRange(userId, startDate, endDate)
            }

            if (localData.isNotEmpty()) {
                _schedules.value = localData
            }

            if (!NetworkUtils.isOnline(context)) {
                _loading.value = false
                return@launch
            }

            // ===== FETCH API =====
            try {
                val response = ApiClient.apiService.getUserSchedules(userId)

                if (!response.isSuccessful) {
                    _error.value = "HTTP ${response.code()}"
                    return@launch
                }

                val body = response.body() ?: return@launch
                if (!body.success) return@launch

                // ===== CONVERT ShiftDay → UserSchedule =====
                val apiSchedules = body.data.flatMap { day ->
                    day.sessions.map { session ->
                        UserSchedule(
                            nid = 0,
                            nuserid = userId,
                            dwork = day.date,
                            dstart = session.start,
                            dend = session.end,
                            nidsched = 0,
                            cschedname = day.shiftName
                        )
                    }
                }

                val filtered = apiSchedules.filter {
                    val d = LocalDate.parse(it.dwork)
                    !d.isBefore(LocalDate.parse(startDate)) &&
                            !d.isAfter(LocalDate.parse(endDate))
                }

                withContext(Dispatchers.IO) {
                    dao.deleteOutOfRange(userId, startDate, endDate)
                    filtered.forEach { dao.insert(it) }
                }

                _schedules.value = filtered

            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.localizedMessage
            } finally {
                _loading.value = false
            }
        }
    }
}
