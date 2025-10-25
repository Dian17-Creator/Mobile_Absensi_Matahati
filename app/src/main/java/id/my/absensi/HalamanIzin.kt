package id.my.matahati.absensi

import okhttp3.RequestBody.Companion.toRequestBody
import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.json.JSONObject

class HalamanIzin : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    HalamanIzinUI()
                }
            }
        }
    }
}

@Composable
fun HalamanIzinUI() {
    val activity = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()

    val date = remember { mutableStateOf("") }
    val locationName = remember { mutableStateOf("Mencari lokasi...") } // tampil di UI
    val coordinate = remember { mutableStateOf("") } // dikirim ke server
    val reason = remember { mutableStateOf("") }
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val message = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val errorDetail = remember { mutableStateOf<String?>(null) }
    val primaryColor = Color(0xFFFF6F51)

    // Set tanggal otomatis
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        date.value = sdf.format(Date())
    }

    // Ambil lokasi otomatis pakai Nominatim API
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
                    coordinate.value = "${loc.latitude},${loc.longitude}"

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
                                val obj = JSONObject(json)
                                val displayName = obj.optString("display_name", "")
                                withContext(Dispatchers.Main) {
                                    locationName.value =
                                        if (displayName.isNotBlank()) displayName
                                        else coordinate.value
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    locationName.value = coordinate.value
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HalamanIzin", "Nominatim error: ${e.message}")
                            withContext(Dispatchers.Main) {
                                locationName.value = coordinate.value
                            }
                        }
                    }
                } else {
                    locationName.value = "Lokasi tidak tersedia"
                }
            }
        } else {
            ActivityCompat.requestPermissions(activity, permissions, 1001)
            locationName.value = "Menunggu izin lokasi..."
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = File(activity.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { activity.finish() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.Black)
            }
            Text(
                text = "Izin Tidak Masuk",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF333333)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preview Foto
        photoUri.value?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = "Foto izin",
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { launcher.launch(null) },
            enabled = !isLoading.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6F51),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Ambil Foto")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Field Tanggal
        OutlinedTextField(
            value = date.value,
            onValueChange = {},
            label = { Text("Tanggal") },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Field Lokasi (alamat readable dari Nominatim)
        OutlinedTextField(
            value = "📍 ${locationName.value}",
            onValueChange = {},
            label = { Text("Lokasi") },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Alasan
        OutlinedTextField(
            value = reason.value,
            onValueChange = { reason.value = it },
            label = { Text("Alasan Izin") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                focusedLabelColor = primaryColor,
                cursorColor = primaryColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    if (reason.value.isBlank()) {
                        message.value = "Mohon isi alasan izin"
                        return@launch
                    }
                    if (photoUri.value == null) {
                        message.value = "Mohon ambil foto terlebih dahulu"
                        return@launch
                    }

                    isLoading.value = true
                    message.value = "Mengirim permintaan izin..."
                    errorDetail.value = null

                    val result = uploadRequest(
                        date = date.value,
                        coordinate = coordinate.value,
                        placeName = locationName.value,
                        reason = reason.value.trim(),
                        photoUri = photoUri.value!!,
                        context = activity
                    )

                    isLoading.value = false
                    message.value =
                        if (result.success) "✅ ${result.message}" else "❌ ${result.message}"
                    errorDetail.value = result.errorDetail
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
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

/* Upload + Helper tetap sama */
/* -------------------------------
   🔹 Bagian Upload dan Helper
-------------------------------- */
suspend fun uploadRequest(
    date: String,
    coordinate: String, // kirim nlat,nlng
    placeName: String,  // kirim cplacename
    reason: String,
    photoUri: Uri,      // URI dari gambar
    context: Context    // context untuk ambil session dll
): UploadResult = withContext(Dispatchers.IO) {
    try {
        Log.d("HalamanIzin", "uploadRequest: date=$date loc=$coordinate name=$placeName reasonLen=${reason.length}")

        val session = SessionManager(context.applicationContext)
        val userIdFromSession = session.getUserId()
        val activity = context as? ComponentActivity
        val userId = if (userIdFromSession != -1) {
            userIdFromSession
        } else {
            activity?.intent?.getIntExtra("USER_ID", -1) ?: -1
        }

        if (userId == -1) {
            return@withContext UploadResult(false, "⚠️ Gagal mengirim izin: user tidak ditemukan", "User ID invalid")
        }

        val file = getFileFromUri(photoUri, context)
        if (file == null || !file.exists()) {
            return@withContext UploadResult(false, "Foto tidak ditemukan", "Path: ${photoUri.path}")
        }

        val bytes = file.readBytes()
        val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

        // ✅ JSON body termasuk placeName
        val jsonBody = """
        {
            "userId": "$userId",
            "requestDate": "$date",
            "location": "$coordinate",
            "placeName": "$placeName",
            "reason": "$reason",
            "photoBase64": "$base64Image"
        }
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://absensi.matahati.my.id/user_request_mobile.php")
            .post(body)
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        val statusCode = response.code

        Log.d("HalamanIzin", "HTTP Status: $statusCode")
        Log.d("HalamanIzin", "Server response: $responseBody")

        if (statusCode == 200) {
            parseJsonResponse(responseBody)
        } else {
            UploadResult(false, "HTTP $statusCode Error", responseBody)
        }

    } catch (e: Exception) {
        Log.e("HalamanIzin", "uploadRequest error", e)
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
        Log.e("HalamanIzin", "Error getFileFromUri", e)
        null
    }
}

private fun parseJsonResponse(jsonString: String): UploadResult {
    return try {
        if (jsonString.contains("<!DOCTYPE HTML") || jsonString.contains("<html>")) {
            return UploadResult(false, "Server mengembalikan halaman HTML", "Endpoint salah / bukan JSON.")
        }

        val success = jsonString.contains("\"success\":true") || jsonString.contains("'success':true")
        val messageMatch = """"message"\s*:\s*"([^"]*)"""".toRegex().find(jsonString)
        val message = messageMatch?.groupValues?.get(1)
            ?: if (success) "Request berhasil" else "Request gagal"

        UploadResult(success, message)
    } catch (e: Exception) {
        UploadResult(false, "Gagal parsing JSON", e.message)
    }
}
