package id.my.matahati.absensi.data

import android.app.Application
import android.util.Log
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

            try {
                // Ambil data lokal dulu
                val localData = withContext(Dispatchers.IO) {
                    dao.getLogsForUser(userId)
                }

                if (localData.isNotEmpty()) {
                    _logs.value = localData
                    Log.d("ABSENSI_LOGS", "Loaded ${localData.size} local logs")
                }

                if (NetworkUtils.isOnline(context)) {
                    val response = ApiClient.apiService.getLogsByUser(userId)
                    if (response.isSuccessful) {
                        val remoteLogs = response.body() ?: emptyList()

                        if (remoteLogs.isNotEmpty()) {
                            val mappedLogs = remoteLogs.map { api ->
                                AbsensiLog(
                                    id = api.nid,
                                    user_id = api.nuserId,
                                    waktu = api.dscanned,
                                    scan = "${api.nlat ?: "-"}, ${api.nlng ?: "-"}",
                                    approved_by = api.nadminid?.toString() ?: "-"
                                )
                            }

                            // Simpan ke DB di thread IO
                            withContext(Dispatchers.IO) {
                                dao.insertAll(mappedLogs)
                            }

                            // Update UI di thread utama
                            _logs.value = mappedLogs
                            Log.d("ABSENSI_LOGS", "Fetched ${mappedLogs.size} remote logs")
                        } else {
                            _error.value = "Data log kosong dari server"
                            Log.w("ABSENSI_LOGS", "Empty data from API")
                        }
                    } else {
                        _error.value = "HTTP ${response.code()} - Gagal memuat data log"
                        Log.e("ABSENSI_LOGS", "HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } else {
                    if (localData.isEmpty()) {
                        _error.value = "Tidak ada koneksi dan data lokal kosong"
                    }
                }
            } catch (e: Exception) {
                Log.e("ABSENSI_LOGS", "Error: ${e.message}", e)
                _error.value = "Gagal mengambil data: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _loading.value = false
            }
        }
    }
}
