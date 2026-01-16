@file:OptIn(ExperimentalMaterial3Api::class)
package id.my.matahati.absensi

import okhttp3.*
import java.util.*
import java.io.File
import android.net.Uri
import android.Manifest
import android.util.Log
import android.os.Bundle
import org.json.JSONObject
import android.widget.Toast
import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import coil.compose.rememberAsyncImagePainter
import id.my.matahati.absensi.data.OfflineIzin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import id.my.matahati.absensi.utils.NetworkUtils
import androidx.compose.foundation.verticalScroll
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.material3.DropdownMenuItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ArrowBack
import com.google.android.gms.location.LocationServices
import androidx.compose.material3.ExposedDropdownMenuBox
import id.my.matahati.absensi.worker.enqueueIzinSyncWorker
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

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
    val localeId = Locale("id", "ID")
    val displayFormat = SimpleDateFormat("dd-MM-yyyy", localeId)
    val valueFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val activity = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()

    val locationName = remember { mutableStateOf("Mencari lokasi...") }
    val coordinate = remember { mutableStateOf("") }
    val reason = remember { mutableStateOf("") }
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val message = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val errorDetail = remember { mutableStateOf<String?>(null) }
    val category = remember { mutableStateOf("") }
    val primaryColor = Color(0xFFB63352)

    //untuk kategori absen
    val reqType = remember {mutableStateOf("reguler")} //kategori izin reguler | request

    //Format backend
    val startDateValue = remember {mutableStateOf("")}
    val endDateValue = remember {mutableStateOf("")}

    //Format Frontend
    val startDateDisplay = remember{mutableStateOf("")}
    val endDateDisplay = remember{mutableStateOf("")}

    //Launch effect untuk tanggal
    LaunchedEffect(Unit) {
        val now = Date()
        startDateValue.value = valueFormat.format(now)
        endDateValue.value = valueFormat.format(now)

        startDateDisplay.value = displayFormat.format(now)
        endDateDisplay.value = displayFormat.format(now)
    }

    val showStartPicker = remember { mutableStateOf(false) }
    val showEndPicker = remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    if (showStartPicker.value) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = startDatePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Date(millis)
                        startDateDisplay.value = displayFormat.format(date)
                        startDateValue.value = valueFormat.format(date)

                        if (reqType.value == "reguler") {
                            endDateDisplay.value = startDateDisplay.value
                            endDateValue.value = startDateValue.value
                        }
                    }
                    showStartPicker.value = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker.value = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    //Lalu untuk end datenya
    if (showEndPicker.value) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = endDatePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Date(millis)
                        endDateDisplay.value = displayFormat.format(date)
                        endDateValue.value = valueFormat.format(date)
                    }
                    showEndPicker.value = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker.value = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }


    DisposableEffect(Unit) {

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: android.content.Intent?) {
                Toast.makeText(activity, "✅ Izin offline berhasil dikirim!", Toast.LENGTH_SHORT).show()
                val intentToHome = android.content.Intent(activity, MainActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                activity.startActivity(intentToHome)
                activity.finishAffinity()
            }
        }

        val filter = android.content.IntentFilter("SYNC_IZIN_SUCCESS")
        androidx.core.content.ContextCompat.registerReceiver(
            activity, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED

        )

        onDispose {
            try { activity.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
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
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB63352),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Ambil Foto")
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(6.dp))

        // 🔽 Dropdown Kategori
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // =========================
            // KATEGORI (KIRI)
            // =========================
            var expandedKategori by remember { mutableStateOf(false) }
            val kategoriList = listOf("Izin", "Sakit")

            ExposedDropdownMenuBox(
                expanded = expandedKategori,
                onExpandedChange = { expandedKategori = !expandedKategori },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = category.value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori") },
                    textStyle = TextStyle(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedKategori)
                                Icons.Filled.KeyboardArrowUp
                            else
                                Icons.Filled.KeyboardArrowDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedKategori,
                    onDismissRequest = { expandedKategori = false }
                ) {
                    kategoriList.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                category.value = item
                                expandedKategori = false
                            }
                        )
                    }
                }
            }

            // =========================
            // TIPE ABSEN (KANAN)
            // =========================
            var expandedType by remember { mutableStateOf(false) }
            val typeList = listOf("Reguler", "Multi Tanggal")

            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = !expandedType },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = if (reqType.value == "reguler") "Reguler" else "Multi Tanggal",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipe Absen") },
                    textStyle = TextStyle(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedType)
                                Icons.Filled.KeyboardArrowUp
                            else
                                Icons.Filled.KeyboardArrowDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    typeList.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                reqType.value =
                                    if (item == "Reguler") "reguler" else "request"
                                expandedType = false
                            }
                        )
                    }
                }
            }
        }

        //Untuk reguler tanggal
        // 📅 TANGGAL REGULER (1 hari)
        if (reqType.value == "reguler") {

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = startDateDisplay.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tanggal Izin") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Pilih tanggal",
                        modifier = Modifier.clickable {
                            showStartPicker.value = true
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        //Untuk multi tanggal
        if (reqType.value == "request") {

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 📅 Dari Tanggal
                OutlinedTextField(
                    value = startDateDisplay.value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mulai") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pilih tanggal mulai",
                            modifier = Modifier.clickable {
                                showStartPicker.value = true
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // teks "Sampai"
                Text(
                    text = "Sampai",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                // 📅 Sampai Tanggal
                OutlinedTextField(
                    value = endDateDisplay.value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Berakhir") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pilih tanggal akhir",
                            modifier = Modifier.clickable {
                                showEndPicker.value = true
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    if (reqType.value == "reguler") {
                        endDateValue.value = startDateValue.value
                    }
                    isLoading.value = true
                    message.value = "Mengirim permintaan izin..."
                    errorDetail.value = null

                    val result = uploadRequest(
                        coordinate = coordinate.value,
                        placeName = locationName.value,
                        category = category.value,
                        reason = reason.value.trim(),
                        photoUri = photoUri.value!!,
                        reqType = reqType.value,                 // ⬅️
                        startDate = startDateValue.value,        // ⬅️
                        endDate = endDateValue.value,            // ⬅️
                        context = activity
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
                                "✅ Izin berhasil dikirim ke server!"

                            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
                            kotlinx.coroutines.delay(1200)

                            val intent = Intent(activity, MainActivity::class.java)
                            activity.startActivity(intent)
                            activity.finish()
                        }
                    }

                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
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
suspend fun uploadRequest(
    photoUri: Uri,
    reason: String,
    coordinate: String,
    placeName: String,
    category: String,
    reqType: String,
    startDate: String,
    endDate: String,
    context: Context
): UploadResult = withContext(Dispatchers.IO) {
    try {
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

        // 🔹 Cek koneksi internet
        if (!NetworkUtils.isOnline(context)) {
            val izin = OfflineIzin(
                userId = userId,
                coordinate = coordinate,
                date = startDate,
                placeName = placeName,
                category = category,
                reason = reason,
                photoBase64 = base64Image
            )

            MyApp.db.offlineIzinDao().insert(izin)

            enqueueIzinSyncWorker(context) // 🔥 aktifkan worker

            return@withContext UploadResult(
                success = true,
                message = "📡 Data disimpan offline dan akan dikirim otomatis saat online"
            )
        }

        // ✅ JSON body termasuk placeName
        val jsonBody = if (reqType == "request") {
        """
        {
            "userId": "$userId",
            "creqcategory": "request",
            "startDate": "$startDate",
            "endDate": "$endDate",
            "location": "$coordinate",
            "placeName": "$placeName",
            "category": "$category",
            "reason": "$reason",
            "photoBase64": "$base64Image"
        }
        """
        } else {
        """
        {
            "userId": "$userId",
            "creqcategory": "reguler",
            "startDate": "$startDate",
            "endDate": "$endDate",
            "location": "$coordinate",
            "placeName": "$placeName",
            "category": "$category",
            "reason": "$reason",
            "photoBase64": "$base64Image"
        }
        """
        }.trimIndent()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://absensi.matahati.my.id/user_request_mobile_2.php")
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
