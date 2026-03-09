package id.my.matahati.absensi.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.matahati.absensi.MyApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

class SyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = MyApp.db.offlineScanDao()
        val scans = dao.getAll()
        val client = OkHttpClient()

        for (scan in scans) {
            val url = "https://absensi.matahati.my.id/verify.php"
            val json = JSONObject().apply {
                put("token", scan.token)
                put("userId", scan.userId)
                put("lat", scan.lat)
                put("lng", scan.lng)
            }
            val body = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder().url(url).post(body).build()

            try {
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                if (response.isSuccessful) {
                    dao.delete(scan)
                }
            } catch (e: Exception) {
            }
        }
        return Result.success()
    }
}
