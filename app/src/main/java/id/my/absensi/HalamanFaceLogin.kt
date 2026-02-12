@file:OptIn(ExperimentalGetImage::class)

package id.my.matahati.absensi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import android.os.Handler
import android.os.Looper
import android.net.wifi.WifiManager

private const val TAG = "FACE_LOGIN"
private const val API_KEY = "MH4T4H4TI_2025_ABSENSI_APP_SECRETx9P2F7Q1L8S3Z0R6W4K2D1M9B7T5"
private const val FACE_LOGIN_URL = "https://absensi.matahati.my.id/user_face_scan_ssid.php"

private val httpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

fun isFaceValidForLogin(face: Face, allowYaw: Boolean = false): Boolean {
    val yaw = face.headEulerAngleY
    val pitch = face.headEulerAngleX
    val box = face.boundingBox

    if (box.width() < 160 || box.height() < 160) return false

    if (!allowYaw) {
        if (kotlin.math.abs(yaw) > 25) return false
        if (kotlin.math.abs(pitch) > 20) return false
    }

    return true
}

class HalamanFaceLogin : ComponentActivity() {
    private val PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PERMISSIONS.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1001)
        }

        setContent {
            FaceLoginScreen()
        }
    }
}

@Composable
fun FaceLoginScreen() {
    var isSessionStarted by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionManager(context) }
    val userId = session.getUserId()

    var coordinate by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("Mengambil lokasi...") }
    var isCameraReady by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color.Black) }

    LaunchedEffect(Unit) {
        val activity = context as Activity
        val hasLocationPerm =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        if (!hasLocationPerm) {
            placeName = "Lokasi tidak diizinkan"
            return@LaunchedEffect
        }

        try {
            val fused = LocationServices.getFusedLocationProviderClient(activity)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    coordinate = "${loc.latitude},${loc.longitude}"
                    scope.launch(Dispatchers.IO) {
                        val place = reverseGeocode(loc.latitude, loc.longitude)
                        withContext(Dispatchers.Main) {
                            placeName = place.ifEmpty { coordinate }
                        }
                    }
                } else {
                    placeName = "Lokasi tidak tersedia"
                }
            }
        } catch (e: Exception) {
            placeName = "Lokasi gagal diambil"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = { (context as Activity).finish() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFFF6F51))
            }
            Text(
                "FACE LOGIN",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6F51)
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            CameraPreview(
                onReady = { isCameraReady = true },
                onFaceFrame = { face ->
                    if (!isSessionStarted) return@CameraPreview
                    if (!isFaceValidForLogin(face)) return@CameraPreview

                    if (!isCapturing && !isUploading) {
                        isCapturing = true
                        CameraController.capture()
                    }
                },
                onCaptured = { bitmap ->
                    if (bitmap == null) {
                        statusText = "Wajah tidak terdeteksi dengan jelas."
                        statusColor = Color.Red
                        isCapturing = false
                        isSessionStarted = false
                        return@CameraPreview
                    }

                    if (userId <= 0) {
                        statusText = "User tidak valid"
                        statusColor = Color.Red
                        isCapturing = false
                        isSessionStarted = false
                        return@CameraPreview
                    }

                    scope.launch {
                        isUploading = true
                        statusColor = Color.Black

                        val result = uploadFace(context, bitmap, userId, coordinate, placeName)

                        isUploading = false
                        isCapturing = false
                        statusText = result.message
                        statusColor = if (result.success) Color(0xFF2E7D32) else Color.Red

                        if (result.success) {
                            showSuccessDialog = true
                            isSessionStarted = false
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(3f / 4f)
                    .border(3.dp, Color(0xFF82FF5C), RoundedCornerShape(12.dp))
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("📍 $placeName", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        Button(
            enabled = isCameraReady && !isUploading && !isCapturing,
            onClick = {
                isSessionStarted = true
                statusText = ""
                statusColor = Color.Black
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F51))
        ) {
            Text(
                when {
                    isUploading -> "Lihat Ke kamera"
                    isCapturing -> "Lihat Ke kamera"
                    else -> "Mulai Absen"
                }
            )
        }

        if (statusText.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        if (showSuccessDialog) {
            val scale by animateFloatAsState(
                targetValue = if (showSuccessDialog) 1f else 0.9f,
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "scale"
            )

            val alpha by animateFloatAsState(
                targetValue = if (showSuccessDialog) 1f else 0f,
                animationSpec = tween(180),
                label = "alpha"
            )

            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(56.dp)
                        )
                        Text("Absen Berhasil!", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("Kembali ke halaman utama", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        Button(
                            onClick = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                                (context as Activity).finish()
                            },
                            modifier = Modifier.width(200.dp).height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6F51),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("OK")
                        }
                    }
                }
            )
        }
    }
}

object CameraController {
    var capture: () -> Unit = {}
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    onReady: () -> Unit,
    onFaceFrame: (Face) -> Unit,
    onCaptured: (Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val executor = remember {
        Executors.newSingleThreadExecutor()
    }

    DisposableEffect(lifecycleOwner) {

        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                FaceLoginDetector.detector
                    .process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.size == 1) {
                            Handler(Looper.getMainLooper()).post {
                                onFaceFrame(faces.first())
                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        CameraController.capture = {
            imageCapture.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val bmp = imageProxyToBitmap(image)
                            val rotated = rotateBitmap(
                                bmp,
                                image.imageInfo.rotationDegrees.toFloat()
                            )

                            FaceLoginDetector.detector
                                .process(InputImage.fromBitmap(rotated, 0))
                                .addOnSuccessListener { faces ->
                                    if (faces.size != 1) {
                                        onCaptured(null)
                                    } else {
                                        val face = faces.first()
                                        if (!isFaceValidForLogin(face)) {
                                            onCaptured(null)
                                        } else {
                                            onCaptured(
                                                resizeFaceForLogin(
                                                    cropFaceForLogin(rotated, face)
                                                )
                                            )
                                        }
                                    }
                                    image.close()
                                }
                        } catch (e: Exception) {
                            onCaptured(null)
                            image.close()
                        }
                    }
                }
            )
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            imageCapture,
            imageAnalysis
        )

        onReady()

        onDispose {
            cameraProvider.unbindAll()   // 🔥 INI KUNCI ANTI BLANK
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

data class FaceLoginResponse(
    val success: Boolean,
    val message: String,
    val confidence: Double?
)

suspend fun uploadFace(
    context: Context,
    bitmap: Bitmap,
    userId: Int,
    location: String,
    place: String
): FaceLoginResponse = withContext(Dispatchers.IO) {
    try {
        val bytes = compressBitmap(bitmap)

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val rawSsid = wifiManager.connectionInfo?.ssid ?: ""

        val ssid = rawSsid
            .replace("\"", "")
            .takeIf { it != "<unknown ssid>" }
            ?: ""

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api_key", API_KEY)
            .addFormDataPart("userId", userId.toString())
            .addFormDataPart("location", location)
            .addFormDataPart("place", place)
            .addFormDataPart("ssid", ssid)
            .addFormDataPart("facefile", "face.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()

        val req = Request.Builder()
            .url(FACE_LOGIN_URL)
            .post(body)
            .build()

        httpClient.newCall(req).execute().use { resp ->

            val code = resp.code
            val raw = resp.body?.string()

            Log.e("UPLOAD_FACE", "HTTP CODE = $code")
            Log.e("UPLOAD_FACE", "RAW BODY = $raw")

            if (raw.isNullOrBlank()) {
                return@withContext FaceLoginResponse(false, "Server kosong / error $code", null)
            }

            val obj = JSONObject(raw)

            FaceLoginResponse(
                obj.optBoolean("success"),
                obj.optString("message"),
                obj.optDouble("confidence")
            )
        }

    } catch (e: Exception) {
        Log.e("UPLOAD_FACE", "ERROR =", e)
        FaceLoginResponse(false, "Koneksi gagal", null)
    }
}

fun compressBitmap(bitmap: Bitmap): ByteArray {
    val maxW = 600
    val scale = if (bitmap.width > maxW) maxW.toFloat() / bitmap.width else 1f
    val resized = if (scale < 1f)
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    else bitmap
    return ByteArrayOutputStream().apply {
        resized.compress(Bitmap.CompressFormat.JPEG, 75, this)
    }.toByteArray()
}

suspend fun reverseGeocode(lat: Double, lng: Double): String =
    withContext(Dispatchers.IO) {
        try {

            Log.d("REVERSE_DEBUG", "Call SERVER reverse -> LAT=$lat | LON=$lng")

            val url =
                "https://absensi.matahati.my.id/reverse_geocode.php?lat=$lat&lon=$lng"

            val req = Request.Builder()
                .url(url)
                .build()

            httpClient.newCall(req).execute().use { resp ->

                val body = resp.body?.string() ?: ""

                Log.d("REVERSE_DEBUG", "SERVER RESPONSE = $body")

                if (!resp.isSuccessful) {
                    Log.e("REVERSE_DEBUG", "Server error code=${resp.code}")
                    return@withContext ""
                }

                if (!body.trim().startsWith("{")) {
                    Log.e("REVERSE_DEBUG", "Response bukan JSON")
                    return@withContext ""
                }

                val obj = JSONObject(body)
                obj.optString("display_name", "")
            }

        } catch (e: Exception) {
            Log.e("REVERSE_DEBUG", "Reverse error", e)
            ""
        }
    }

fun cropFaceForLogin(bitmap: Bitmap, face: Face): Bitmap {
    val box = face.boundingBox
    val left = box.left.coerceAtLeast(0)
    val top = box.top.coerceAtLeast(0)
    val width = box.width().coerceAtMost(bitmap.width - left)
    val height = box.height().coerceAtMost(bitmap.height - top)
    return Bitmap.createBitmap(bitmap, left, top, width, height)
}

fun resizeFaceForLogin(bitmap: Bitmap, size: Int = 320): Bitmap {
    return Bitmap.createScaledBitmap(bitmap, size, size, true)
}