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
                // 🗂️ Ambil data lokal terlebih dahulu
                val localData = withContext(Dispatchers.IO) {
                    dao.getLogsForUser(userId)
                }

                if (localData.isNotEmpty()) {
                    _logs.value = localData
                    Log.d("ABSENSI_LOGS", "Loaded ${localData.size} local logs")
                }

                // 🌐 Kalau online → ambil dari server
                if (NetworkUtils.isOnline(context)) {
                    val response = ApiClient.apiService.getLogsByUser(userId)

                    if (response.isSuccessful) {
                        val remoteLogs = response.body() ?: emptyList()

                        if (remoteLogs.isNotEmpty()) {
                            // 🔁 Konversi dari model API ke Room model
                            val mappedLogs = remoteLogs.map { api ->
                                AbsensiLog(
                                    id = api.nid,
                                    user_id = api.nuserId,
                                    waktu = api.dscanned,
                                    scan = "${api.nlat ?: "-"}, ${api.nlng ?: "-"}",
                                    approved_by = api.nadminid?.toString() ?: "-",
                                    typeAbsensi = api.typeAbsensi ?: "scan" // 🔥 tambahkan ini!
                                )
                            }


                            // 💾 Simpan ke Room di background
                            withContext(Dispatchers.IO) {
                                dao.insertAll(mappedLogs)
                            }

                            // 🔄 Update UI dengan data terbaru
                            _logs.value = mappedLogs
                            Log.d("ABSENSI_LOGS", "Fetched ${mappedLogs.size} remote logs")
                        } else {
                            _error.value = "⚠️ Data log kosong dari server"
                            Log.w("ABSENSI_LOGS", "Empty response body")
                        }
                    } else {
                        val msg = "HTTP ${response.code()} - Gagal memuat data log"
                        _error.value = msg
                        Log.e("ABSENSI_LOGS", msg)
                    }
                } else {
                    if (localData.isEmpty()) {
                        _error.value = "Tidak ada koneksi dan data lokal kosong"
                    }
                }

            } catch (e: Exception) {
                Log.e("ABSENSI_LOGS", "Error loadLogs(): ${e.message}", e)
                _error.value = "Gagal mengambil data: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _loading.value = false
            }
        }
    }
}
