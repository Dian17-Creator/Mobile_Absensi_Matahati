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

    private val _schedules = MutableStateFlow<List<UserSchedule>>(emptyList())
    val schedules: StateFlow<List<UserSchedule>> = _schedules

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val dao = MyApp.db.userScheduleDao()
    private val context = getApplication<Application>().applicationContext

    /**
     * Load jadwal shift berdasarkan userId (offline-first, ±7 hari)
     */
    fun loadSchedules(userId: Int) {
        enqueueScheduleSyncWorker(context, userId)

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_DATE
            val startDate = today.minusDays(7).format(formatter)
            val endDate = today.plusDays(7).format(formatter)

            // Ambil dari database lokal dulu
            val localData = withContext(Dispatchers.IO) {
                dao.getSchedulesInRange(userId, startDate, endDate)
            }

            if (localData.isNotEmpty()) {
                _schedules.value = localData
            }

            val isConnected = NetworkUtils.isOnline(context)

            if (isConnected) {
                try {
                    val response = ApiClient.apiService.getUserSchedules(userId)
                    if (!response.isNullOrEmpty()) {
                        val filtered = response.filter { schedule ->
                            val date = LocalDate.parse(schedule.dwork, formatter)
                            !date.isBefore(today.minusDays(7)) && !date.isAfter(today.plusDays(7))
                        }

                        // Simpan hasil ke Room (ganti userId jika kosong)
                        withContext(Dispatchers.IO) {
                            filtered.forEach { item ->
                                val fixedItem = if (item.nuserid == 0) item.copy(nuserid = userId) else item
                                dao.insert(fixedItem)
                            }

                            // Bersihkan data di luar 30 hari untuk hemat storage
                            val cleanupStart = today.minusDays(30).format(formatter)
                            dao.deleteOutOfRange(userId, cleanupStart, endDate)
                        }

                        _schedules.value = filtered
                    } else {
                        _error.value = "Data dari server kosong"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _error.value = "Gagal mengambil data dari server: ${e.localizedMessage}"

                    val fallback = withContext(Dispatchers.IO) {
                        dao.getSchedulesForUser(userId)
                    }
                    _schedules.value = fallback
                }
            } else {
                // OFFLINE → tampilkan data lokal
                val offlineData = withContext(Dispatchers.IO) {
                    dao.getSchedulesInRange(userId, startDate, endDate)
                }

                if (offlineData.isEmpty()) {
                    _error.value = "Tidak ada koneksi dan data lokal kosong"
                }

                _schedules.value = offlineData
            }

            _loading.value = false
        }
    }
}
