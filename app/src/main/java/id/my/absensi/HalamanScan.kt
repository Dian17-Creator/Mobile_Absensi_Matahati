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
import id.my.matahati.absensi.data.ScanResult

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

    val primaryColor = Color(0xFFFF6F51)

    val storedUserId = session.getUserId()
    val userIdFromIntent = activity?.intent?.getIntExtra("USER_ID", -1) ?: -1
    val userNameFromIntent = activity?.intent?.getStringExtra("USER_NAME") ?: ""
    val userEmailFromIntent = activity?.intent?.getStringExtra("USER_EMAIL") ?: ""

    val userId = if (storedUserId != -1) storedUserId else userIdFromIntent
    val userName = if (storedUserId != -1) session.getUser()["name"]?.toString() ?: "" else userNameFromIntent
    val userEmail = if (storedUserId != -1) session.getUser()["email"]?.toString() ?: "" else userEmailFromIntent

    // ✅ Observe WorkManager sync result
    DisposableEffect(Unit) {
        val observer = androidx.lifecycle.Observer<List<androidx.work.WorkInfo>> { workInfos ->
            val isSuccess = workInfos.any { it.state == androidx.work.WorkInfo.State.SUCCEEDED }
            if (isSuccess) {
                scanResult = ScanResult.SuccessImage
            }
        }

        workManager.getWorkInfosByTagLiveData("sync_offline_scans")
            .observe(lifecycleOwner, observer)

        onDispose {
            workManager.getWorkInfosByTagLiveData("sync_offline_scans")
                .removeObserver(observer)
        }
    }

    // Jika ada perubahan dari luar (misal dari HomeFragment)
    LaunchedEffect(externalScanResult) {
        externalScanResult?.let { result ->
            scanResult = result
            when (result) {
                is ScanResult.SuccessImage -> showCamera = false
                is ScanResult.Message -> showCamera = true
                else -> {}
            }
        }
    }

    // 🔹 Layout responsif
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        val screenHeight = maxHeight
        val cameraHeight = screenHeight * 0.45f
        val imageSize = screenHeight * 0.45f
        val buttonHeight = screenHeight * 0.07f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Judul
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Silahkan Scan QR Code",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(top = 30.dp)
                )
            }

            // 🔹 Kamera
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
                                sendToVerify(context, extractedToken) { result ->
                                    scanResult = result
                                }
                            } else {
                                scanResult = ScanResult.Message("❌ QR tidak valid (tidak ada token).")
                            }
                        }
                    )
                }
            } else if (!hasCameraPermission) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Izin kamera/lokasi belum diberikan")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onRequestPermission() }) {
                        Text("Berikan Izin")
                    }
                }
            }

            // 🔹 Hasil Scan
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
                    Text(
                        text = "Menunggu jaringan...",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }

                is ScanResult.SuccessImage -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.goodwork),
                        contentDescription = "Scan berhasil",
                        modifier = Modifier.size(imageSize)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan berhasil",
                        fontSize = 16.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            // 🔹 Tombol Logout
            Button(
                onClick = { LogoutHelper.logout(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Logout")
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
                    val userId = session.getUserId()

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
