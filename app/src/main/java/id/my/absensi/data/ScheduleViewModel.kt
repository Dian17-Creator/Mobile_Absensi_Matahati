package id.my.matahati.absensi.data

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.matahati.absensi.MyApp
import id.my.matahati.absensi.worker.enqueueScheduleSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
     * Load jadwal shift berdasarkan userId (offline-first)
     */
    fun loadSchedules(userId: Int) {
        enqueueScheduleSyncWorker(context, userId)

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            // 1️⃣ Coba ambil data lokal dulu
            val localData = dao.getSchedulesForUser(userId)
            if (localData.isNotEmpty()) {
                _schedules.value = localData
            }

            // 2️⃣ Cek apakah ada koneksi internet
            val isConnected = isInternetAvailable()

            if (isConnected) {
                try {
                    val response = ApiClient.apiService.getUserSchedules(userId)
                    if (!response.isNullOrEmpty()) {
                        // simpan ke Room
                        response.forEach { dao.insert(it) }
                        _schedules.value = response
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _error.value = "Gagal mengambil data dari server: ${e.localizedMessage}"
                    // tetap pakai data lokal
                    _schedules.value = dao.getSchedulesForUser(userId)
                }
            } else {
                // tidak ada koneksi
                if (localData.isEmpty()) {
                    _error.value = "Tidak ada koneksi internet dan data lokal kosong"
                }
                _schedules.value = localData
            }

            _loading.value = false
        }
    }

    /**
     * Cek koneksi internet (WiFi / Data)
     */
    private fun isInternetAvailable(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
