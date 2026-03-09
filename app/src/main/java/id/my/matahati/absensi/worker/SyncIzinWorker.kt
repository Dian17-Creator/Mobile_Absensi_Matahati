package id.my.matahati.absensi.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import id.my.matahati.absensi.MyApp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class SyncIzinWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = runBlocking {
        try {
            val dao = MyApp.db.offlineIzinDao()
            val list = dao.getAll()

            if (list.isEmpty()) {
                Log.d("SyncIzinWorker", "📭 Tidak ada data izin offline untuk dikirim.")
                return@runBlocking Result.success()
            }

            val client = OkHttpClient()

            for (izin in list) {
                // ===========================
                // 🌍 REVERSE GEOCODING JIKA placeName MASIH KOORDINAT
                // ===========================
                // ===========================
// 🌍 REVERSE GEOCODING VIA SERVER
// ===========================
                var placeNameToSend = izin.placeName

                if (placeNameToSend.matches(Regex("-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?"))) {
                    try {
                        val latLng = izin.coordinate.split(",")

                        if (latLng.size == 2) {
                            val lat = latLng[0].trim()
                            val lng = latLng[1].trim()

                            val url =
                                "https://absensi.matahati.my.id/reverse_geocode.php?lat=$lat&lon=$lng"

                            Log.d("SyncIzinWorker", "🌍 CALL SERVER: $url")

                            val geocodeRequest = Request.Builder()
                                .url(url)
                                .build()

                            client.newCall(geocodeRequest).execute().use { geocodeResponse ->

                                val json = geocodeResponse.body?.string() ?: ""

                                Log.d("SyncIzinWorker", "🌍 SERVER RESPONSE: $json")

                                if (geocodeResponse.isSuccessful && json.startsWith("{")) {

                                    val obj = JSONObject(json)
                                    val displayName = obj.optString("display_name", "")

                                    if (displayName.isNotBlank()) {
                                        placeNameToSend = displayName
                                        Log.d("SyncIzinWorker", "📍 Reverse OK: $placeNameToSend")
                                    }
                                }
                            }
                        }

                    } catch (e: Exception) {
                        Log.w("SyncIzinWorker", "Reverse gagal: ${e.message}")
                    }
                }

                // ===========================
                // 📡 SIAPKAN JSON UNTUK DIKIRIM
                // ===========================
                val jsonBody = """
                    {
                        "userId": "${izin.userId}",
                        "requestDate": "${izin.date}",
                        "location": "${izin.coordinate}",
                        "placeName": "$placeNameToSend",
                        "reason": "${izin.reason}",
                        "photoBase64": "${izin.photoBase64}"
                    }
                """.trimIndent()

                val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://absensi.matahati.my.id/user_request_mobile.php")
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                val success = response.isSuccessful && responseBody.contains("\"success\":true")

                if (success) {
                    dao.deleteById(izin.id)
                    Log.d("SyncIzinWorker", "✅ Izin ${izin.id} berhasil dikirim")

                    // ✅ Notifikasi kecil ke user (opsional)
                    android.os.Handler(applicationContext.mainLooper).post {
                        android.widget.Toast.makeText(
                            applicationContext,
                            "✅ Izin offline (${izin.date}) berhasil dikirim!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Log.e("SyncIzinWorker", "❌ Gagal kirim izin ${izin.id}: $responseBody")
                }
            }

            // ===========================
            // 🔔 KIRIM BROADCAST KE UI
            // ===========================
            val intent = android.content.Intent("SYNC_IZIN_SUCCESS")
            applicationContext.sendBroadcast(intent)
            Log.d("SyncIzinWorker", "📤 Broadcast SYNC_IZIN_SUCCESS dikirim ke UI.")

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncIzinWorker", "⚠️ Error: ${e.message}")
            Result.retry()
        }
    }
}
