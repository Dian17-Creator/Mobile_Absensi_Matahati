package id.my.matahati.absensi

import androidx.compose.ui.draw.clip
import android.content.Intent
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.camera.core.ExperimentalGetImage
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*
import id.my.matahati.absensi.data.OfflineScan
import id.my.matahati.absensi.worker.enqueueSyncWorker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import id.my.matahati.absensi.data.ScanResult
import java.util.Locale

class HalamanScan : ComponentActivity() {

    private var hasAllPermissions by mutableStateOf(false)

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasAllPermissions = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasAllPermissions) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                100
            )
        }

        setContent {
            HalamanScanUI(
                hasCameraPermission = hasAllPermissions,
                onRequestPermission = {
                    ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        100
                    )
                }
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            hasAllPermissions = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            setContent {
                HalamanScanUI(
                    hasCameraPermission = hasAllPermissions,
                    onRequestPermission = {
                        ActivityCompat.requestPermissions(
                            this,
                            REQUIRED_PERMISSIONS,
                            100
                        )
                    }
                )
            }
        }
    }
}
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalamanScanUI(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    externalScanResult: ScanResult? = null
) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val workManager = androidx.work.WorkManager.getInstance(context)

    var scanResult by remember { mutableStateOf<ScanResult>(ScanResult.Message("Arahkan kamera ke QR Code")) }
    var showCamera by remember { mutableStateOf(true) }

    val primaryColor = Color(0xFFFF6F51) // 🔸 warna oranye khas

    // 🔹 Data user
    val storedUserId = session.getUserId()
    val userIdFromIntent = activity?.intent?.getIntExtra("USER_ID", -1) ?: -1
    val userNameFromIntent = activity?.intent?.getStringExtra("USER_NAME") ?: ""
    val userEmailFromIntent = activity?.intent?.getStringExtra("USER_EMAIL") ?: ""

    val userId = if (storedUserId != -1) storedUserId else userIdFromIntent
    val userName = if (storedUserId != -1) session.getUser()["name"]?.toString() ?: "" else userNameFromIntent
    val userEmail = if (storedUserId != -1) session.getUser()["email"]?.toString() ?: "" else userEmailFromIntent

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // ✅ Observe WorkManager sync result
    DisposableEffect(Unit) {
        val observer = androidx.lifecycle.Observer<List<androidx.work.WorkInfo>> { workInfos ->
            val isSuccess = workInfos.any { it.state == androidx.work.WorkInfo.State.SUCCEEDED }
            if (isSuccess) scanResult = ScanResult.SuccessImage
        }

        workManager.getWorkInfosByTagLiveData("sync_offline_scans")
            .observe(lifecycleOwner, observer)

        onDispose {
            workManager.getWorkInfosByTagLiveData("sync_offline_scans")
                .removeObserver(observer)
        }
    }

    // Jika ada perubahan dari luar
    LaunchedEffect(externalScanResult) {
        externalScanResult?.let { result ->
            scanResult = result
            showCamera = result !is ScanResult.SuccessImage
        }
    }

    // 🌈 Layout utama dua lapisan
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(screenHeight * 0.25f)
                .background(color = Color(0xFFFF6F51))
        )

    // 🔹 Konten utama di atas background
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 30.dp)
        ) {
            val screenHeight = maxHeight
            val cameraHeight = screenHeight * 0.35f
            val imageSize = screenHeight * 0.3f
            val buttonHeight = screenHeight * 0.07f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 🔹 Card waktu (di atas background oranye)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4C4C59)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    CardWaktu()
                }

                // 🔹 Card shift
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    CardShift(userId = userId)
                }

                // 🔹 Kamera dan hasil scan
                if (hasCameraPermission && showCamera) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cameraHeight)
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onScan = { rawValue ->
                                var extractedToken: String? = null
                                try {
                                    val obj = JSONObject(rawValue)
                                    extractedToken = obj.optString("token", null)
                                } catch (e: Exception) {
                                    extractedToken = rawValue
                                }

                                if (extractedToken != null) {
                                    showCamera = false
                                    sendToVerify(context, extractedToken) { result -> scanResult = result }
                                } else {
                                    scanResult = ScanResult.Message("❌ QR tidak valid (tidak ada token).")
                                }
                            }
                        )
                    }
                }
                // 🔹 Hasil scan
                when (scanResult) {
                    is ScanResult.Message -> Text(
                        text = (scanResult as ScanResult.Message).text,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )

                    is ScanResult.WaitingImage -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.nointernet),
                            contentDescription = "Menunggu jaringan",
                            modifier = Modifier.size(imageSize)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Menunggu jaringan...", fontSize = 14.sp, color = Color.Gray)
                    }

                    is ScanResult.SuccessImage -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.goodwork),
                            contentDescription = "Scan berhasil",
                            modifier = Modifier.size(imageSize)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Scan berhasil", fontSize = 14.sp, color = Color(0xFF4CAF50))
                    }
                }

                // 🔹 Tombol sejajar
                // 🔹 Tombol sejajar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(context, HalamanIzin::class.java).apply {
                                putExtra("USER_ID", userId)
                                putExtra("USER_NAME", userName)
                                putExtra("USER_EMAIL", userEmail)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6F51),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Izin Tidak Masuk")
                    }

                    Button(
                        onClick = { LogoutHelper.logout(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFC0000),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Logout")
                    }
                }

                Button(
                    onClick = {
                        val intent = Intent(context, HalamanManual::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4C4C59),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Absen Manual")
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onScan: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = Executors.newSingleThreadExecutor()
    val scanner = BarcodeScanning.getClient()

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (rawValue != null) {
                                        try {
                                            cameraProvider.unbindAll()
                                        } catch (e: Exception) {
                                            Log.e("CameraPreview", "unbind failed", e)
                                        }
                                        onScan(rawValue)
                                        break
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Log.e("QR_SCAN", "Gagal scan", it)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

@SuppressLint("MissingPermission")
fun sendToVerify(context: Context, token: String, onResult: (ScanResult) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        onResult(ScanResult.Message("❌ Izin lokasi tidak diberikan"))
        return
    }

    // 🔁 retry cari lokasi hingga dapat
    fun tryGetLocation(retry: Int = 0) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                    val lat = location.latitude
                    val lng = location.longitude
                    val session = SessionManager(context)

                    // ✅ FIX fallback userId seperti di UbahPassword
                    val userId = session.getUserId().takeIf { it != -1 }
                        ?: (context as? ComponentActivity)?.intent?.getIntExtra("USER_ID", -1) ?: -1

                    if (userId == -1) {
                        onResult(ScanResult.Message("⚠️ User belum terdeteksi, silakan login ulang"))
                        return@addOnSuccessListener  // ✅ perbaikan utama
                    }

                    val scanRecord = OfflineScan(
                        token = token,
                        userId = userId,
                        lat = lat,
                        lng = lng
                    )

                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val isConnected = cm.activeNetworkInfo?.isConnected == true

                    if (!isConnected) {
                        CoroutineScope(Dispatchers.IO).launch {
                            MyApp.db.offlineScanDao().insert(scanRecord)
                            enqueueSyncWorker(context)
                            withContext(Dispatchers.Main) {
                                onResult(ScanResult.WaitingImage)
                            }
                        }
                    } else {
                        sendOnline(context, scanRecord, onResult)
                    }
                } else if (retry < 5) {
                    // 🔁 coba ulang tiap 1 detik
                    Handler(Looper.getMainLooper()).postDelayed({
                        tryGetLocation(retry + 1)
                    }, 1000)
                } else {
                    onResult(ScanResult.Message("❌ Gagal mendapatkan lokasi"))
                }
            }
            .addOnFailureListener {
                onResult(ScanResult.Message("❌ Gagal membaca lokasi: ${it.message}"))
            }
    }

    tryGetLocation()
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

/* 🔹 CARD WAKTU (real-time) */
@Composable
fun CardWaktu() {
    var waktuSekarang by remember { mutableStateOf("") }
    var tanggalSekarang by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalDateTime.now()
            val formatterJam = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            val formatterTgl = java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy",
                Locale("id", "ID")
            )
            waktuSekarang = now.format(formatterJam)
            tanggalSekarang = now.format(formatterTgl)
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = waktuSekarang,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFFFFFFFF)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tanggalSekarang,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp
            ),
            color = Color(0xFFFFFFFF)
        )
    }
}

/* 🔹 CARD SHIFT (warna & periode dinamis) */
@Composable
fun CardShift(
    userId: Int,
    scheduleViewModel: id.my.matahati.absensi.data.ScheduleViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val schedules by scheduleViewModel.schedules.collectAsState(initial = emptyList())
    val today = java.time.LocalDate.now()

    LaunchedEffect(Unit) {
        if (userId != -1) scheduleViewModel.loadSchedules(userId)
    }

    val todayShift = schedules.find { it.dwork == today.toString() }
    val shiftColor = todayShift?.let { generateColorFromShift(it.cschedname) } ?: Color(0xFFF5F5F5)

    // 🔹 cari periode tanggal berturut-turut dengan shift sama
    val sameShiftDates = schedules
        .asSequence()
        .filter { it.cschedname == todayShift?.cschedname }
        .mapNotNull { runCatching { java.time.LocalDate.parse(it.dwork) }.getOrNull() }
        .distinct()
        .toList()
        .sorted()

    val runs = mutableListOf<MutableList<java.time.LocalDate>>()
    for (d in sameShiftDates) {
        if (runs.isEmpty()) {
            runs.add(mutableListOf(d))
        } else {
            val lastRun = runs.last()
            val lastDate = lastRun.last()
            if (lastDate.plusDays(1) == d) lastRun.add(d)
            else runs.add(mutableListOf(d))
        }
    }

    val containingRun = runs.find { it.contains(today) } ?: emptyList()
    val minDate = containingRun.minOrNull()
    val maxDate = containingRun.maxOrNull()

    val shortFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM", Locale("id", "ID"))
    val yearFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy", Locale("id", "ID"))
    val periodeText = if (minDate != null && maxDate != null) {
        if (minDate.year == maxDate.year)
            "${minDate.format(shortFormatter)} - ${maxDate.format(shortFormatter)} ${maxDate.format(yearFormatter)}"
        else
            "${minDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))} - " +
                    "${maxDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))}"
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = shiftColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Shift Hari Ini",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (todayShift != null) Color.White else Color.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (todayShift != null) {
                Text(
                    text = "${todayShift.cschedname.replaceFirstChar { it.titlecase(Locale("id", "ID")) }} " +
                            "(${todayShift.dstart} - ${todayShift.dend})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )

                periodeText?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Periode : $it",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            } else {
                Text(
                    text = "Belum ada shift untuk hari ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

