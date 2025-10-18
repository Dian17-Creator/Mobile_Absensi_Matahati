package id.my.matahati.absensi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.content.ContextCompat
import id.my.absensi.ui.CameraPage
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream


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
    val coroutineScope = rememberCoroutineScope()

    // ✅ state untuk kamera
    var showCamera by remember { mutableStateOf(false) }
    var photoFile by remember { mutableStateOf<File?>(null) }

    val date = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("Mencari lokasi...") }
    val reason = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val errorDetail = remember { mutableStateOf<String?>(null) }
    val primaryColor = Color(0xFFFF6F51)

    // 🗓️ Set tanggal otomatis
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        date.value = sdf.format(Date())
    }

    // 📍 Ambil lokasi otomatis
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
                location.value =
                    if (loc != null) "${loc.latitude},${loc.longitude}" else "Lokasi tidak tersedia"
            }
        } else {
            ActivityCompat.requestPermissions(activity, permissions, 1001)
            location.value = "Menunggu izin lokasi..."
        }
    }

    // 📸 Jika user membuka kamera
    if (showCamera) {
        CameraPage(
            onPhotoCaptured = { file ->
                photoFile = file
                showCamera = false
            },
            onCancel = { showCamera = false }
        )
        return
    }

    // UI utama absen manual
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 🔹 Header
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
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

        // 🔹 Preview foto jika sudah diambil
        photoFile?.let {
            Image(
                painter = rememberAsyncImagePainter(Uri.fromFile(it)),
                contentDescription = "Foto absen manual",
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 Tombol buka kamera depan
        Button(
            onClick = { showCamera = true },
            enabled = !isLoading.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (photoFile == null) "Ambil Foto (Kamera Depan)" else "Ambil Ulang Foto")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Input alasan absen manual
        OutlinedTextField(
            value = reason.value,
            onValueChange = { reason.value = it },
            label = { Text("Alasan Absen Manual") },
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

        // 🔹 Tombol submit data
        Button(
            onClick = {
                coroutineScope.launch {
                    if (reason.value.isBlank()) {
                        message.value = "Mohon isi alasan absen"
                        return@launch
                    }
                    if (photoFile == null) {
                        message.value = "Mohon ambil foto terlebih dahulu"
                        return@launch
                    }

                    isLoading.value = true
                    message.value = "Mengirim data absen..."
                    errorDetail.value = null

                    val result = uploadAbsenManual(
                        date.value,
                        location.value,
                        reason.value.trim(),
                        Uri.fromFile(photoFile),
                        activity
                    )

                    isLoading.value = false
                    message.value = if (result.success)
                        "✅ ${result.message}" else "❌ ${result.message}"
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

        // 🔹 Pesan status
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
    val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
        ?: return originalFile // fallback jika gagal decode

    val maxSize = 1080
    val scale = maxOf(bitmap.width, bitmap.height).toFloat() / maxSize
    val scaledBitmap = if (scale > 1) {
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width / scale).toInt(),
            (bitmap.height / scale).toInt(),
            true
        )
    } else bitmap

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
    location: String,
    reason: String,
    photoUri: Uri,
    context: Context
): UploadResult = withContext(Dispatchers.IO) {
    try {
        val session = SessionManager(context.applicationContext)
        val userId = session.getUserId()
        if (userId == -1) return@withContext UploadResult(false, "User ID tidak ditemukan")

        val (lat, lng) = if (location.contains(",")) {
            val parts = location.split(",")
            Pair(parts[0].trim(), parts[1].trim())
        } else Pair("0", "0")

        // 🔹 Ambil file asli dari URI
        val file = getFileFromUri(photoUri, context)
        if (file == null || !file.exists()) {
            return@withContext UploadResult(false, "File foto tidak ditemukan")
        }

        // 🔹 Kompres gambar sebelum upload
        val compressedFile = compressImageFile(file, context)
        Log.d("HalamanManual", "Ukuran sebelum: ${file.length()/1024} KB, sesudah: ${compressedFile.length()/1024} KB")

        // 🔹 Encode ke Base64
        val bytes = compressedFile.readBytes()
        val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

        // 🔹 Buat JSON body
        val jsonBody = """
        {
            "nuserId": "$userId",
            "nlat": "$lat",
            "nlng": "$lng",
            "creason": "$reason",
            "photoBase64": "$base64Image"
        }
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://absensi.matahati.my.id/mscan_manual.php")
            .post(body)
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        val statusCode = response.code

        Log.d("HalamanManual", "HTTP Status: $statusCode")
        Log.d("HalamanManual", "Server response: $responseBody")

        if (statusCode == 200) {
            parseJsonResponse(responseBody)
        } else {
            UploadResult(false, "HTTP $statusCode Error", responseBody)
        }

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
