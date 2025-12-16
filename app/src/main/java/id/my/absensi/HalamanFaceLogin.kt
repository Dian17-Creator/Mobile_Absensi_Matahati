@file:OptIn(ExperimentalGetImage::class)

package id.my.matahati.absensi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/* ================= CONFIG ================= */

private const val TAG = "FACE_LOGIN"
private const val API_KEY = "MH4T4H4TI_2025_ABSENSI_APP_SECRETx9P2F7Q1L8S3Z0R6W4K2D1M9B7T5"
private const val FACE_LOGIN_URL =
    "https://absensi.matahati.my.id/user_face_scan_mobile.php"

private val httpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

/* ================= ACTIVITY ================= */

class HalamanFaceLogin : ComponentActivity() {

    private val PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PERMISSIONS.all {
                ContextCompat.checkSelfPermission(this, it) ==
                        PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1001)
        }

        setContent {
            FaceLoginScreen()
        }
    }
}

/* ================= UI ================= */

@Composable
fun FaceLoginScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionManager(context) }

    val userId = session.getUserId()

    var coordinate by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("Mengambil lokasi...") }

    var isLocationReady by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    var statusText by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color.Black) }

    /* ===== GPS (WAJIB SEBELUM CAPTURE) ===== */
    LaunchedEffect(Unit) {
        val activity = context as Activity

        val hasLocationPerm =
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPerm) {
            placeName = "Izin lokasi belum diberikan"
            return@LaunchedEffect
        }

        val fused = LocationServices.getFusedLocationProviderClient(activity)

        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    coordinate = "${loc.latitude},${loc.longitude}"

                    scope.launch(Dispatchers.IO) {
                        val place = reverseGeocode(loc.latitude, loc.longitude)
                        withContext(Dispatchers.Main) {
                            placeName = place.ifEmpty { coordinate }
                            isLocationReady = true
                        }
                    }
                } else {
                    placeName = "Lokasi tidak tersedia"
                }
            }
        } catch (e: SecurityException) {
            placeName = "Akses lokasi ditolak"
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        /* ===== HEADER ===== */
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
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

        /* ===== CAMERA ===== */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {

            CameraPreview(
                onReady = { isCameraReady = true },
                onCaptured = { bitmap ->
                    scope.launch {
                        isUploading = true
                        statusText = "Memindai wajah..."
                        statusColor = Color.Black

                        val result = uploadFace(
                            bitmap = bitmap,
                            userId = userId,
                            location = coordinate,
                            place = placeName
                        )

                        isUploading = false
                        statusText = result.message
                        statusColor =
                            if (result.success) Color(0xFF2E7D32) else Color.Red

                        if (result.success) {
                            delay(2500)
                            (context as Activity).finish()
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(3f / 4f)
                    .border(3.dp, Color(0xFFFF6F51), RoundedCornerShape(12.dp))
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "📍 $placeName",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Button(
            enabled = isCameraReady && isLocationReady && !isUploading,
            onClick = {
                CameraController.capture()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6F51)
            )
        ) {
            Text(
                when {
                    !isLocationReady -> "Menunggu GPS..."
                    isUploading -> "Memindai..."
                    else -> "Ambil Foto"
                }
            )
        }

        if (statusText.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                statusText,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ================= CAMERA ================= */

object CameraController {
    var capture: () -> Unit = {}
}

@Composable
fun CameraPreview(
    onReady: () -> Unit,
    onCaptured: (Bitmap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            ProcessCameraProvider.getInstance(ctx).addListener({
                val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                CameraController.capture = {
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bmp = imageProxyToBitmap(image)
                                val rotated =
                                    rotateBitmap(bmp, image.imageInfo.rotationDegrees.toFloat())
                                val fixed = unMirrorBitmap(rotated)
                                image.close()
                                onCaptured(fixed)
                            }
                        }
                    )
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture
                )

                onReady()

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

/* ================= NETWORK ================= */
data class FaceLoginResponse(
    val success: Boolean,
    val message: String,
    val confidence: Double?
)

suspend fun uploadFace(
    bitmap: Bitmap,
    userId: Int,
    location: String,
    place: String
): FaceLoginResponse = withContext(Dispatchers.IO) {
    try {
        val bytes = compressBitmap(bitmap)

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api_key", API_KEY)
            .addFormDataPart("userId", userId.toString())
            .addFormDataPart("location", location)
            .addFormDataPart("place", place)
            .addFormDataPart(
                "facefile",
                "face.jpg",
                bytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val req = Request.Builder()
            .url(FACE_LOGIN_URL)
            .post(body)
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val json = resp.body?.string() ?: ""
            val obj = JSONObject(json)
            FaceLoginResponse(
                obj.optBoolean("success"),
                obj.optString("message"),
                obj.optDouble("confidence")
            )
        }
    } catch (e: Exception) {
        FaceLoginResponse(false, "Koneksi gagal", null)
    }
}

/* ================= UTIL ================= */

fun compressBitmap(bitmap: Bitmap): ByteArray {
    val maxW = 600
    val scale = if (bitmap.width > maxW) maxW.toFloat() / bitmap.width else 1f
    val resized =
        if (scale < 1f)
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

fun unMirrorBitmap(bitmap: Bitmap): Bitmap {
    val matrix = Matrix().apply { preScale(-1f, 1f) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
suspend fun reverseGeocode(lat: Double, lng: Double): String =
    withContext(Dispatchers.IO) {
        try {
            val url =
                "https://nominatim.openstreetmap.org/reverse" +
                        "?lat=$lat&lon=$lng&format=json&addressdetails=0"

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "MatahatiAbsensiMobileApp/1.0")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return@withContext ""
                val obj = JSONObject(body)
                obj.optString("display_name", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocode error", e)
            ""
        }
    }
