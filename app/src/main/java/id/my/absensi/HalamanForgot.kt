package id.my.matahati.absensi

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.foundation.shape.RoundedCornerShape

class HalamanForgot : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    HalamanForgotUI()
                }
            }
        }
    }
}

@Composable
fun HalamanForgotUI() {
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val date = remember { mutableStateOf("") }
    val time = remember { mutableStateOf("") }
    val reason = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("Mencari lokasi...") }
    val coords = remember { mutableStateOf(Pair("0", "0")) }
    val photoUri = remember { mutableStateOf<Uri?>(null) }

    val message = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val primaryColor = Color(0xFFB63352)

    /* 📍 Lokasi */
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(activity, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            location.value = "Izin lokasi belum diberikan"
            return@LaunchedEffect
        }

        val fused = LocationServices.getFusedLocationProviderClient(activity)
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                coords.value = Pair(
                    loc.latitude.toString(),
                    loc.longitude.toString()
                )

                // default fallback
                location.value = "${loc.latitude}, ${loc.longitude}"

                scope.launch(Dispatchers.IO) {
                    try {
                        val client = OkHttpClient()
                        val url =
                            "https://nominatim.openstreetmap.org/reverse" +
                                    "?lat=${loc.latitude}&lon=${loc.longitude}" +
                                    "&format=json&addressdetails=1"

                        val request = Request.Builder()
                            .url(url)
                            .addHeader(
                                "User-Agent",
                                "MatahatiApp/1.0 (mailto:admin@matahati.my.id)"
                            )
                            .build()

                        val response = client.newCall(request).execute()
                        val json = response.body?.string()

                        if (response.isSuccessful && json != null) {
                            val obj = org.json.JSONObject(json)
                            val displayName = obj.optString("display_name", "")

                            withContext(Dispatchers.Main) {
                                if (displayName.isNotBlank()) {
                                    location.value = displayName
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // fallback tetap koordinat
                    }
                }
            } else {
                location.value = "Lokasi tidak tersedia"
            }
        }
    }

    /* 📸 Kamera */
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val file = File(activity.cacheDir, "forgot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            photoUri.value = Uri.fromFile(file)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        /* 🔙 Header */
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = { activity.finish() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Text(
                "Lupa Absen",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        photoUri.value?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { launcher.launch(null) },
            modifier = Modifier.fillMaxWidth().height(45.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text(if (photoUri.value == null) "Ambil Foto" else "Ambil Ulang Foto")
        }

        Spacer(Modifier.height(16.dp))

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

        OutlinedTextField(
            value = reason.value,
            onValueChange = { reason.value = it },
            label = { Text("Alasan Lupa Absen") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                focusedLabelColor = primaryColor,
                cursorColor = primaryColor
            ),
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    if (date.value.isBlank() || time.value.isBlank()) {
                        message.value = "Tanggal dan jam wajib diisi"
                        return@launch
                    }
                    if (photoUri.value == null) {
                        message.value = "Foto wajib diambil"
                        return@launch
                    }

                    isLoading.value = true
                    val result = uploadAbsenForgot(
                        date.value,
                        time.value,
                        coords.value.first,
                        coords.value.second,
                        location.value,
                        reason.value,
                        photoUri.value!!,
                        context
                    )
                    isLoading.value = false
                    message.value = result.message

                    if (result.success) {
                        Toast.makeText(context, "Absen Berhasil Dikirim", Toast.LENGTH_LONG).show()
                        activity.finish()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(45.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor
            ),
            enabled = !isLoading.value
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text("Mengirim...")
            } else {
                Text("Submit")
            }
        }

        if (message.value.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(message.value, color = Color.Red)
        }
    }
}

suspend fun uploadAbsenForgot(
    date: String,
    time: String,
    lat: String,
    lng: String,
    place: String,
    reason: String,
    photoUri: Uri,
    context: Context
): UploadResult = withContext(Dispatchers.IO) {

    try {
        val session = SessionManager(context)
        val userId = session.getUserId()
        if (userId == -1) return@withContext UploadResult(false, "User tidak valid")

        val file = File(photoUri.path!!)
        val bytes = file.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val json = """
        {
          "nuserId": "$userId",
          "dscanned": "$date",
          "dtime": "$time",
          "nlat": "$lat",
          "nlng": "$lng",
          "cplacename": "$place",
          "creason": "$reason",
          "photoBase64": "$base64"
        }
        """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://absensi.matahati.my.id/user_scan_forgot_mobile.php")
            .post(body)
            .build()

        val resp = OkHttpClient().newCall(request).execute()
        val respBody = resp.body?.string() ?: ""

        if (resp.isSuccessful && respBody.contains("\"success\":true")) {
            UploadResult(true, "Pengajuan lupa absen berhasil")
        } else {
            UploadResult(false, "Gagal mengirim data")
        }

    } catch (e: Exception) {
        UploadResult(false, "Koneksi gagal")
    }
}


