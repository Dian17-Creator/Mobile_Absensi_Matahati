package id.my.matahati.absensi.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.matahati.absensi.MyApp
import id.my.matahati.absensi.data.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val userId = inputData.getInt("USER_ID", -1)
        if (userId == -1) return@withContext Result.failure()

        if (!isInternetAvailable()) {
            // Kalau belum online, jadwalkan ulang
            return@withContext Result.retry()
        }

        try {
            val api = ApiClient.apiService
            val dao = MyApp.db.userScheduleDao()
            val response = api.getUserSchedules(userId)

            if (!response.isNullOrEmpty()) {
                // Hapus data lama dan simpan yang baru
                response.forEach { dao.insert(it) }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // coba lagi nanti kalau gagal
        }
    }

    private fun isInternetAvailable(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
