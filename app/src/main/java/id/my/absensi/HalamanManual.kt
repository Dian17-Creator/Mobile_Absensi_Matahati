package id.my.matahati.absensi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import id.my.matahati.absensi.worker.enqueueManualSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import id.my.matahati.absensi.utils.NetworkUtils
import id.my.matahati.absensi.data.OfflineManualAbsen
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.content.Intent

class HalamanManual : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    HalamanManualUI()
                }
            }
        }
    }
}

@Composable
fun HalamanManualUI() {
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val location = remember { mutableStateOf("Mencari lokasi...") }
    val coords = remember { mutableStateOf(Pair("0", "0")) } // ✅ koordinat asli
    val reason = remember { mutableStateOf("") }
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val message = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val errorDetail = remember { mutableStateOf<String?>(null) }
    val primaryColor = Color(0xFFB63352)

    val cal = Calendar.getInstance()

    val date = remember {
        mutableStateOf(
            "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        )
    }

    val time = remember {
        mutableStateOf(
            "%02d:%02d:00".format(
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE)
            )
        )
    }

    // ✅ Receiver untuk mendeteksi jika data offline berhasil dikirim
    DisposableEffect(Unit) {
        // ✅ Buat receiver
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: android.content.Intent?) {
                Log.d("HalamanManual", "📡 Broadcast diterima: kembali ke halaman scan")
                val intentToScan = android.content.Intent(activity, HalamanScan::class.java)
                intentToScan.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                activity.startActivity(intentToScan)
                activity.finish()
            }
        }

        // ✅ Buat filter broadcast
        val filter = android.content.IntentFilter("SYNC_MANUAL_ABSEN_SUCCESS")

        try {
            // ✅ Gunakan flag baru agar tidak crash di Android 13+
            ContextCompat.registerReceiver(
                activity,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

        } catch (e: Exception) {
            // fallback untuk versi Android lama
            activity.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                activity.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w("HalamanManual", "Receiver sudah dilepas: ${e.message}")
            }
        }
    }


    // 🗓️ Set tanggal otomatis
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        date.value = sdf.format(Date())
    }

    // 📍 Ambil lokasi otomatis + nama lokasi via Nominatim
    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(activity, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    coords.value = Pair(loc.latitude.toString(), loc.longitude.toString())
                    location.value = "${loc.latitude},${loc.longitude}"

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val client = OkHttpClient()
                            val url =
                                "https://nominatim.openstreetmap.org/reverse?lat=${loc.latitude}&lon=${loc.longitude}&format=json&addressdetails=1"

                            val request = Request.Builder()
                                .url(url)
                                .addHeader("User-Agent", "MatahatiApp/1.0 (mailto:admin@matahati.my.id)")
                                .build()

                            val response = client.newCall(request).execute()
                            val json = response.body?.string()
                            if (response.isSuccessful && json != null) {
                                val obj = org.json.JSONObject(json)
                                val displayName = obj.optString("display_name", "")
                                withContext(Dispatchers.Main) {
                                    if (displayName.isNotBlank()) {
                                        location.value = displayName
                                    } else {
                                        location.value = "${loc.latitude},${loc.longitude}"
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HalamanManual", "Reverse geocode error: ${e.message}")
                        }
                    }
                } else {
                    location.value = "Lokasi tidak tersedia"
                }
            }
        } else {
            ActivityCompat.requestPermissions(activity, permissions, 1001)
            location.value = "Menunggu izin lokasi..."
        }
    }

    // 📸 Kamera
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = File(activity.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            photoUri.value = Uri.fromFile(file)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(onClick = { activity.finish() }, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.Black)
            }
            Text(
                text = "Absen Manual",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF333333)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preview foto
        photoUri.value?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Foto absen manual",
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tombol ambil foto
        Button(
            onClick = { launcher.launch(null) },
            enabled = !isLoading.value,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (photoUri.value == null) "Ambil Foto" else "Ambil Ulang Foto")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = date.value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tanggal Absen") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                focusedLabelColor = primaryColor,
                cursorColor = primaryColor
            ),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            date.value = String.format("%04d-%02d-%02d", y, m + 1, d)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Icon(Icons.Default.DateRange, null)
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = time.value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Jam Absen") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                focusedLabelColor = primaryColor,
                cursorColor = primaryColor
            ),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            time.value = String.format("%02d:%02d:00", h, m)
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        true
                    ).show()
                }) {
                    Icon(Icons.Default.Schedule, null)
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // Alasan
        OutlinedTextField(
            value = reason.value,
            onValueChange = { reason.value = it },
            label = { Text("Alasan Absen Manual") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                focusedLabelColor = primaryColor,
                cursorColor = primaryColor
            ),
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Submit
        Button(
            onClick = {
                coroutineScope.launch {
                    if (reason.value.isBlank()) {
                        message.value = "Mohon isi alasan absen"
                        return@launch
                    }
                    if (photoUri.value == null) {
                        message.value = "Mohon ambil foto terlebih dahulu"
                        return@launch
                    }

                    isLoading.value = true
                    message.value = "Mengirim data absen..."
                    errorDetail.value = null

                    val result = uploadAbsenManual(
                        date.value,
                        time.value,
                        coords.value.first,
                        coords.value.second,
                        location.value,
                        reason.value.trim(),
                        photoUri.value!!,
                        activity
                    )

                    isLoading.value = false
                    message.value =
                        if (result.success) "✅ ${result.message}" else "❌ ${result.message}"
                    errorDetail.value = result.errorDetail

                    // ✅ Tambahan: jika hasil berhasil, arahkan ke MainActivity
                    if (result.success || result.message.contains("offline", true)) {
                        withContext(Dispatchers.Main) {
                            val msg = if (result.message.contains("offline", true))
                                "📡 Data disimpan offline. Akan dikirim otomatis saat online!"
                            else
                                "✅ Data berhasil dikirim ke server!"

                            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()

                            // 🕒 Delay sedikit agar user bisa lihat toast
                            kotlinx.coroutines.delay(1200)

                            // 🔁 Kembali ke halaman scan
                            val intent = Intent(activity, MainActivity::class.java)
                            activity.startActivity(intent)
                            activity.finish()
                        }
                    }

                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            enabled = !isLoading.value,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4C4C59),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mengirim...")
            } else {
                Text("Submit")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status
        if (message.value.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message.value,
                    color = when {
                        message.value.startsWith("✅") -> Color(0xFF2ECC71)
                        message.value.startsWith("❌") -> Color.Red
                        else -> Color(0xFFF39C12)
                    },
                    fontWeight = FontWeight.Medium
                )
                errorDetail.value?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

data class UploadResult(
    val success: Boolean,
    val message: String,
    val errorDetail: String? = null
)

fun compressImageFile(originalFile: File, context: Context): File {
    val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath) ?: return originalFile
    val maxSize = 1080
    val scale = maxOf(bitmap.width, bitmap.height).toFloat() / maxSize
    val scaledBitmap = if (scale > 1)
        Bitmap.createScaledBitmap(bitmap, (bitmap.width / scale).toInt(), (bitmap.height / scale).toInt(), true)
    else bitmap

    val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
    FileOutputStream(compressedFile).use { out ->
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
    }
    if (!bitmap.isRecycled) bitmap.recycle()
    if (scaledBitmap != bitmap && !scaledBitmap.isRecycled) scaledBitmap.recycle()
    return compressedFile
}

suspend fun uploadAbsenManual(
    date: String,
    time: String,
    lat: String,
    lng: String,
    cplacename: String,
    reason: String,
    photoUri: Uri,
    context: Context
): UploadResult = withContext(Dispatchers.IO) {
    try {
        val session = SessionManager(context.applicationContext)
        val userId = session.getUserId()
        if (userId == -1) return@withContext UploadResult(false, "User ID tidak ditemukan")

        val file = getFileFromUri(photoUri, context)
        if (file == null || !file.exists()) {
            return@withContext UploadResult(false, "File foto tidak ditemukan")
        }

        val compressedFile = compressImageFile(file, context)
        val bytes = compressedFile.readBytes()
        val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

        // 🔹 Cek koneksi internet
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val isConnected = cm.activeNetworkInfo?.isConnected == true

        if (!NetworkUtils.isOnline(context)) {
            val absen = OfflineManualAbsen(
                userId = userId,
                date = date,
                lat = lat,
                lng = lng,
                cplacename = cplacename,
                reason = reason,
                photoBase64 = base64Image,
                createdAt = System.currentTimeMillis()
            )

            MyApp.db.offlineManualAbsenDao().insert(absen)

            // 🔥 PENTING: ini yang menjadwalkan sinkronisasi otomatis
            enqueueManualSyncWorker(context)

            return@withContext UploadResult(true, "📡 Data disimpan offline dan akan dikirim otomatis")
        }

        // 🔹 Online Mode
        val jsonBody = """
        {
            "nuserId": "$userId",
            "dscanned": "$date $time",
            "nlat": "$lat",
            "nlng": "$lng",
            "cplacename": "${cplacename.replace("\"", "'")}",
            "creason": "$reason",
            "photoBase64": "$base64Image"
        }
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://absensi.matahati.my.id/mscan_manual_mobile.php")
            .post(body)
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        val statusCode = response.code

        Log.d("HalamanManual", "HTTP Status: $statusCode")
        Log.d("HalamanManual", "Server response: $responseBody")

        if (statusCode == 200) parseJsonResponse(responseBody)
        else UploadResult(false, "HTTP $statusCode Error", responseBody)
    } catch (e: Exception) {
        Log.e("HalamanManual", "uploadAbsenManual error", e)
        UploadResult(false, "Gagal koneksi ke server", e.message)
    }
}
private fun getFileFromUri(uri: Uri, context: Context): File? {
    return try {
        when (uri.scheme) {
            "file" -> File(uri.path ?: "")
            "content" -> {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { output ->
                    inputStream?.copyTo(output)
                }
                inputStream?.close()
                file
            }
            else -> null
        }
    } catch (e: Exception) {
        Log.e("HalamanManual", "Error getFileFromUri", e)
        null
    }
}

private fun parseJsonResponse(jsonString: String): UploadResult {
    return try {
        if (jsonString.contains("<!DOCTYPE HTML") || jsonString.contains("<html>")) {
            return UploadResult(
                success = false,
                message = "Server mengembalikan halaman HTML",
                errorDetail = "Kemungkinan endpoint salah atau bukan JSON."
            )
        }

        var success = false
        var message = "Unknown response"

        if (jsonString.contains("\"success\":true") || jsonString.contains("'success':true")) {
            success = true
        }

        val messageMatch = """"message"\s*:\s*"([^"]*)"""".toRegex().find(jsonString)
        message = messageMatch?.groupValues?.get(1)
            ?: if (success) "Request berhasil" else "Request gagal"

        UploadResult(success, message)
    } catch (e: Exception) {
        UploadResult(
            success = false,
            message = "Gagal parsing respons JSON",
            errorDetail = e.message
        )
    }
}
