package id.my.matahati.absensi.data

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import id.my.matahati.absensi.MyApp
import id.my.matahati.absensi.data.ScanResult
import id.my.matahati.absensi.SessionManager
import id.my.matahati.absensi.worker.enqueueSyncWorker
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.content.pm.PackageManager


@Suppress("MissingPermission")
fun sendToVerify(context: Context, token: String, onResult: (ScanResult) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        onResult(ScanResult.Message("❌ Izin lokasi tidak diberikan"))
        return
    }

    fusedLocationClient.getCurrentLocation(
        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
        null
    ).addOnSuccessListener { location ->
        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            val session = SessionManager(context)

            // ✅ FIX: gunakan getUserId()
            val userId = session.getUserId()

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val isConnected = cm.activeNetworkInfo?.isConnected == true

            val scanRecord = OfflineScan(
                token = token,
                userId = userId,   // ← sekarang aman
                lat = lat,
                lng = lng
            )

            if (!isConnected) {
                CoroutineScope(Dispatchers.IO).launch {
                    MyApp.db.offlineScanDao().insert(scanRecord)
                    enqueueSyncWorker(context)
                    withContext(Dispatchers.Main) {
                        onResult(ScanResult.WaitingImage)
                    }

                    // cek jaringan berkala
                    while (true) {
                        delay(5000)
                        val cmCheck = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val online = cmCheck.activeNetworkInfo?.isConnected == true
                        if (online) {
                            sendOnline(context, scanRecord, onResult)
                            break
                        }
                    }
                }
            } else {
                sendOnline(context, scanRecord, onResult)
            }
        } else {
            onResult(ScanResult.Message("❌ Lokasi tidak tersedia"))
        }
    }.addOnFailureListener {
        onResult(ScanResult.Message("❌ Gagal membaca lokasi : ${it.message}"))
    }
}


fun sendOnline(context: Context, scan: OfflineScan, onResult: (ScanResult) -> Unit) {
    val client = OkHttpClient()
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

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            (context as ComponentActivity).runOnUiThread {
                onResult(ScanResult.WaitingImage)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                (context as ComponentActivity).runOnUiThread {
                    onResult(ScanResult.SuccessImage)
                }
            } else {
                (context as ComponentActivity).runOnUiThread {
                    onResult(ScanResult.Message("❌ Scan gagal"))
                }
            }
        }
    })
}
