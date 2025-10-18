package id.my.matahati.absensi.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.matahati.absensi.MyApp
import id.my.matahati.absensi.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AbsensiViewModel(application: Application) : AndroidViewModel(application) {
    private val _logs = MutableStateFlow<List<AbsensiLog>>(emptyList())
    val logs: StateFlow<List<AbsensiLog>> = _logs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val dao = MyApp.db.absensiLogDao()
    private val context = getApplication<Application>().applicationContext

    fun loadLogs(userId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val localData = withContext(Dispatchers.IO) {
                dao.getLogsForUser(userId)
            }

            if (localData.isNotEmpty()) _logs.value = localData

            val isConnected = NetworkUtils.isOnline(context)
            if (isConnected) {
                try {
                    val response = ApiClient.apiService.getLogsByUser(userId)
                    val remoteLogs = response.data

                    // ✅ Ubah dari JSON ke entity Room
                    val mappedLogs = remoteLogs.map { api ->
                        AbsensiLog(
                            id = api.nid,
                            user_id = api.nuserId,
                            waktu = api.dscanned,
                            scan = "${api.nlat ?: "-"}, ${api.nlng ?: "-"}",
                            approved_by = api.nadminid?.toString() ?: "-"
                        )
                    }

                    if (mappedLogs.isNotEmpty()) {
                        _logs.value = mappedLogs
                        withContext(Dispatchers.IO) {
                            dao.insertAll(mappedLogs)
                        }
                    } else {
                        _error.value = "Data log kosong dari server"
                    }

                } catch (e: Exception) {
                    _error.value = "Gagal mengambil data: ${e.localizedMessage}"
                }
            } else {
                if (localData.isEmpty()) {
                    _error.value = "Tidak ada koneksi dan data lokal kosong"
                }
                _logs.value = localData
            }

            _loading.value = false
        }
    }
}
