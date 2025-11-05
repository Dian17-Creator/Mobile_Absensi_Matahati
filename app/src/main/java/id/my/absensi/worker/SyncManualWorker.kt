package id.my.matahati.absensi.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.matahati.absensi.MyApp
import id.my.matahati.absensi.data.OfflineManualAbsen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class SyncManualWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = MyApp.db.offlineManualAbsenDao()
            val dataList = dao.getAll()

            if (dataList.isEmpty()) {
                Log.d("SyncManualWorker", "✅ Tidak ada data offline untuk disinkronkan")
                return@withContext Result.success()
            }

            val client = OkHttpClient()

            for (absen in dataList) {
                val jsonBody = """
                {
                    "nuserId": "${absen.userId}",
                    "nlat": "${absen.lat}",
                    "nlng": "${absen.lng}",
                    "cplacename": "${absen.cplacename.replace("\"", "'")}",
                    "creason": "${absen.reason}",
                    "photoBase64": "${absen.photoBase64}"
                }
                """.trimIndent()

                val request = Request.Builder()
                    .url("https://absensi.matahati.my.id/mscan_manual.php")
                    .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                Log.d("SyncManualWorker", "Response: $body")

                if (response.isSuccessful) {
                    dao.delete(absen)
                }
            }

            // ✅ Tambahkan notifikasi / toast setelah semua data terkirim
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "📤 Data absen manual offline berhasil dikirim ke server!",
                    Toast.LENGTH_LONG
                ).show()

                // 🛰️ Kirim broadcast ke Activity
                val intent = android.content.Intent("SYNC_MANUAL_ABSEN_SUCCESS")
                context.sendBroadcast(intent)
            }

            Result.success()

        } catch (e: Exception) {
            Log.e("SyncManualWorker", "❌ Error saat sync manual absen", e)
            Result.retry()
        }
    }
}
