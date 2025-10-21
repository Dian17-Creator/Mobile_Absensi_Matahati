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

            if (response.isSuccessful) {
                val schedules = response.body() ?: emptyList()

                if (schedules.isNotEmpty()) {
                    // Hapus data lama dulu
                    dao.deleteAllForUser(userId)

                    // Simpan data baru
                    schedules.forEach { item ->
                        val fixedItem = if (item.nuserid == 0) item.copy(nuserid = userId) else item
                        dao.insert(fixedItem)
                    }
                }

                Result.success()
            } else {
                // Kalau gagal (misal 404/500), coba lagi nanti
                Result.retry()
            }

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
