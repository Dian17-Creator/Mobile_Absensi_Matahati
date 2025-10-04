package id.my.matahati.absensi


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

// 🔹 sealed class untuk hasil scan
sealed class ScanResult {
    data class Message(val text: String) : ScanResult()
    object SuccessImage : ScanResult()
    object WaitingImage : ScanResult()
}

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

@Composable
fun HalamanScanUI(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var scanResult by remember { mutableStateOf<ScanResult>(ScanResult.Message("Arahkan kamera ke QR Code")) }
    var showCamera by remember { mutableStateOf(true) }

    val primaryColor = Color(0xFFFF6F51)

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Silahkan Scan Barcode",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 32.dp)
                )

                if (hasCameraPermission && showCamera) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(425.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                // tampilkan hasil scan
                when (scanResult) {
                    is ScanResult.Message -> Text(
                        text = (scanResult as ScanResult.Message).text,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    is ScanResult.WaitingImage -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.noconnection), // 🔹 gambar menunggu, taruh di drawable
                            contentDescription = "Menunggu jaringan",
                            modifier = Modifier.size(350.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Menunggu jaringan...",
                            fontSize = 20.sp,
                            color = Color.Gray
                        )
                    }

                    is ScanResult.SuccessImage -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.good),
                            contentDescription = "Scan berhasil",
                            modifier = Modifier.size(350.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan berhasil",
                            fontSize = 20.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }


                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Halaman Ubah Password
                    Button(
                        onClick = {
                            val intent = Intent(context, UbahPassword::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ubah Password")
                    }

                    // Halaman Jadwal
                    Button(
                        onClick = {
                            val intent = Intent(context, HalamanJadwal::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Halaman Jadwal")
                    }

                    // Button untuk logout dari sistem
                    Button(
                        onClick = { LogoutHelper.logout(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
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

    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        null
    ).addOnSuccessListener { location ->
        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            val session = SessionManager(context)
            val userId = session.getUserId()

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val isConnected = cm.activeNetworkInfo?.isConnected == true

            val scanRecord = OfflineScan(
                token = token,
                userId = userId,
                lat = lat,
                lng = lng
            )

            if (!isConnected) {
                CoroutineScope(Dispatchers.IO).launch {
                    MyApp.Companion.db.offlineScanDao().insert(scanRecord)
                    enqueueSyncWorker(context)
                    withContext(Dispatchers.Main) {
                        onResult(ScanResult.WaitingImage)
                    }

                    // 🔹 loop cek jaringan
                    while (true) {
                        delay(5000) // cek tiap 5 detik
                        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val online = cm.activeNetworkInfo?.isConnected == true
                        if (online) {
                            // kalau sudah online → kirim ulang
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
