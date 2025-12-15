package id.my.matahati.absensi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

private const val TAG_FACE_LOGIN = "FACE_LOGIN"
private val httpClientLogin by lazy { OkHttpClient() }

data class FaceLoginResponse(
    val success: Boolean,
    val message: String,
    val confidence: Double?
)

class HalamanFaceLogin : ComponentActivity() {

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 11)
        }

        setContent {
            FaceLoginScreen()
        }
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
}
private const val API_KEY = "MH4T4H4TI_2025_ABSENSI_APP_SECRETx9P2F7Q1L8S3Z0R6W4K2D1M9B7T5"
private const val FACE_LOGIN_URL = "https://absensi.matahati.my.id/user_face_scan_mobile.php" // sesuaikan jika berbeda
suspend fun loginWithFaceMultipart(
    context: android.content.Context,
    bitmap: Bitmap,
    userId: Int,
    location: String = "", // "lat,lng" atau "" jika belum ada
    place: String = ""     // nama/alamat hasil reverse geocode (opsional)
): FaceLoginResponse = withContext(Dispatchers.IO) {
    try {
        // compress & resize sebelum dikirim (maxWidth 600, quality 75)
        val bytes = compressBitmapForUpload(bitmap, maxWidth = 600, quality = 75)

        // build multipart body
        val requestBodyBuilder = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)

        // add text parts
        requestBodyBuilder.addFormDataPart("api_key", API_KEY)
        requestBodyBuilder.addFormDataPart("userId", userId.toString())
        requestBodyBuilder.addFormDataPart("location", location)
        requestBodyBuilder.addFormDataPart("place", place)

        // add file part - "facefile"
        val fileRequestBody = okhttp3.RequestBody.create("image/jpeg".toMediaType(), bytes)
        requestBodyBuilder.addFormDataPart("facefile", "face.jpg", fileRequestBody)

        val requestBody = requestBodyBuilder.build()

        val request = Request.Builder()
            .url(FACE_LOGIN_URL)
            .post(requestBody)
            .build()

        httpClientLogin.newCall(request).execute().use { resp ->
            val code = resp.code
            val respBody = resp.body?.string() ?: ""
            Log.d(TAG_FACE_LOGIN, "LOGIN HTTP CODE = $code")
            Log.d(TAG_FACE_LOGIN, "LOGIN RESPONSE = $respBody")

            if (!resp.isSuccessful || respBody.isEmpty()) {
                return@withContext FaceLoginResponse(
                    success = false,
                    message = "Koneksi gagal. Silahkan Coba lagi.",
                    confidence = null
                )
            }

            val obj = JSONObject(respBody)
            val success = obj.optBoolean("success", false)
            val message = obj.optString("message", "Terjadi kesalahan.")
            val confidenceRaw = obj.optDouble("confidence", Double.NaN)
            val confidence = if (confidenceRaw.isNaN()) null else confidenceRaw

            FaceLoginResponse(success, message, confidence)
        }
    } catch (e: Exception) {
        Log.e(TAG_FACE_LOGIN, "Exception loginWithFaceMultipart", e)
        FaceLoginResponse(
            success = false,
            message = "Sistem error. Coba lagi.",
            confidence = null
        )
    }
}

@Composable
fun FaceLoginScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionManager(context) }
    var coordinate by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("Mencari lokasi...") }

    LaunchedEffect(Unit) {
        val activity = context as Activity
        val fused = LocationServices.getFusedLocationProviderClient(activity)

        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val granted = perms.all {
            ContextCompat.checkSelfPermission(activity, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

        if (!granted) {
            placeName = "Izin lokasi belum diberikan"
            ActivityCompat.requestPermissions(activity, perms, 2001)
            return@LaunchedEffect
        }

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                coordinate = "${loc.latitude},${loc.longitude}"

                scope.launch(Dispatchers.IO) {
                    try {
                        val url = "https://nominatim.openstreetmap.org/reverse?lat=${loc.latitude}&lon=${loc.longitude}&format=json&addressdetails=1"
                        val request = Request.Builder()
                            .url(url)
                            .addHeader("User-Agent", "MatahatiApp/1.0")
                            .build()

                        val response = httpClientLogin.newCall(request).execute()
                        val json = response.body?.string() ?: ""
                        val obj = JSONObject(json)
                        val display = obj.optString("display_name", coordinate)

                        withContext(Dispatchers.Main) {
                            placeName = display   // ← UI UPDATE BERHASIL
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            placeName = coordinate
                        }
                    }
                }
            }
        }
    }

    val storedUserId = session.getUserId()
    val userId = if (storedUserId != -1) {
        storedUserId
    } else {
        (context as? ComponentActivity)
            ?.intent
            ?.getIntExtra("USER_ID", -1) ?: -1
    }

    var statusText by remember { mutableStateOf<String?>(null) }
    var statusColor by remember { mutableStateOf(Color.Black) }
    var isProcessing by remember { mutableStateOf(false) }
    var place by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { (context as? Activity)?.finish() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color(0xFFFF6F51)
                )
            }

            Text(
                text = "FACE LOGIN",
                fontSize = 22.sp,
                color = Color(0xFFFF6F51),
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            CameraPreviewLogin(
                onImageCaptured = { bitmap ->
                    if (userId == -1) {
                        statusColor = Color.Red
                        statusText = "User tidak terdeteksi, silakan login ulang."
                        isProcessing = false
                        return@CameraPreviewLogin
                    }

                    scope.launch {
                        isProcessing = true
                        statusColor = Color.Black
                        statusText = "Memindai wajah..."

                        val loc = getLastKnownLocationSimple(context)
                        place = ""   // reset UI state dulu

                        if (loc.isNotEmpty()) {
                            val parts = loc.split(",")
                            if (parts.size == 2) {
                                val lat = parts[0].toDoubleOrNull()
                                val lng = parts[1].toDoubleOrNull()
                                if (lat != null && lng != null) {
                                    statusText = "Mengambil alamat lokasi..."

                                    val resolvedPlace = reverseGeocode(lat, lng)
                                    place = resolvedPlace
                                    Log.d("PLACE_DEBUG", "UI updated place = $resolvedPlace")
                                }
                            }
                        }

                        val result = loginWithFaceMultipart(context, bitmap, userId, coordinate, placeName)

                        isProcessing = false
                        statusColor = if (result.success) Color(0xFF2E7D32) else Color.Red

                        statusText = if (result.success && result.confidence != null) {
                            "${result.message}\nKecocokan: ${"%.1f".format(result.confidence)}%"
                        } else {
                            result.message
                        }

                        if (result.success) {
                            delay(2500)
                            (context as? Activity)?.finish()
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(3f / 4f)
                    .border(
                        width = 3.dp,
                        color = Color(0xFFFF6F51),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Wajah Harus Penuh (termasuk dagu) Di Dalam Kotak Merah",
            color = Color(0xFFE74C3C),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "📍 $placeName",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (userId == -1) {
                    statusColor = Color.Red
                    statusText = "User tidak terdeteksi, silakan login ulang."
                    return@Button
                }
                isProcessing = true
                statusColor = Color.Black
                statusText = "Memindai wajah..."
                CameraLoginController.capture()
            },
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6F51),
                disabledContainerColor = Color(0xFFCCCCCC)
            )
        ) {
            Text(if (isProcessing) "Memindai..." else "Ambil Foto")
        }

        statusText?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = statusColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

object CameraLoginController {
    var capture: () -> Unit = {}
}
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewLogin(
    onImageCaptured: (Bitmap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val currentOnImageCaptured by rememberUpdatedState(onImageCaptured)

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder().build()

                CameraLoginController.capture = {
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                try {
                                    Log.d(TAG_FACE_LOGIN, "Capture success")

                                    val rawBitmap = imageProxyToBitmap(image)
                                    val rotated = rotateBitmap(
                                        rawBitmap,
                                        image.imageInfo.rotationDegrees.toFloat()
                                    )

                                    val maxWidth = 800
                                    val ratio = maxWidth.toFloat() / rotated.width.toFloat()
                                    val thumb = Bitmap.createScaledBitmap(
                                        rotated,
                                        maxWidth,
                                        (rotated.height * ratio).toInt(),
                                        true
                                    )

                                    currentOnImageCaptured(thumb)
                                } catch (e: Exception) {
                                    Log.e(TAG_FACE_LOGIN, "Error processing capture", e)
                                } finally {
                                    image.close()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e(TAG_FACE_LOGIN, "ImageCapture onError", exception)
                                super.onError(exception)
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

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

suspend fun loginWithFace(
    bitmap: Bitmap,
    userId: Int
): FaceLoginResponse =
    withContext(Dispatchers.IO) {
        try {
            val bytes = compressBitmapForUpload(bitmap, maxWidth = 600, quality = 75)
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val json = """
                {
                  "userId": $userId,
                  "photoBase64": "$base64"
                }
            """.trimIndent()

            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FACE_LOGIN_URL)
                .post(body)
                .build()

            httpClientLogin.newCall(request).execute().use { resp ->
                val code = resp.code
                val respBody = resp.body?.string() ?: ""
                Log.d(TAG_FACE_LOGIN, "LOGIN HTTP CODE = $code")
                Log.d(TAG_FACE_LOGIN, "LOGIN RESPONSE = $respBody")

                if (!resp.isSuccessful || respBody.isEmpty()) {
                    return@withContext FaceLoginResponse(
                        success = false,
                        message = "Koneksi gagal. Silahkan Coba lagi.",
                        confidence = null
                    )
                }

                val obj = JSONObject(respBody)
                val success = obj.optBoolean("success", false)
                val message = obj.optString("message", "Terjadi kesalahan.")
                val confidenceRaw = obj.optDouble("confidence", Double.NaN)
                val confidence = if (confidenceRaw.isNaN()) null else confidenceRaw

                FaceLoginResponse(success, message, confidence)
            }
        } catch (e: Exception) {
            Log.e(TAG_FACE_LOGIN, "Exception loginWithFace", e)
            FaceLoginResponse(
                success = false,
                message = "Sistem error. Coba lagi.",
                confidence = null
            )
        }
    }


fun getLastKnownLocationSimple(context: Context): String {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) return "" // belum ada permission

    val providers = lm.getProviders(true)
    var best: android.location.Location? = null
    for (p in providers) {
        try {
            val l = lm.getLastKnownLocation(p) ?: continue
            if (best == null || l.accuracy < best.accuracy) best = l
        } catch (ex: SecurityException) {
            // ignore
        }
    }
    return if (best != null) "${best.latitude},${best.longitude}" else ""
}

suspend fun reverseGeocode(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
    try {
        val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&zoom=18&addressdetails=0"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "MatahatiAbsensiMobileApp/1.0")
            .get()
            .build()

        httpClientLogin.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: return@withContext ""
            val json = JSONObject(body)
            return@withContext json.optString("display_name", "")
        }
    } catch (e: Exception) {
        Log.e(TAG_FACE_LOGIN, "Reverse geocode error", e)
        return@withContext ""
    }
}

private fun compressBitmapForUpload(bitmap: Bitmap, maxWidth: Int = 600, quality: Int = 75): ByteArray {
    val srcW = bitmap.width
    val srcH = bitmap.height
    val scale = if (srcW > maxWidth) maxWidth.toFloat() / srcW.toFloat() else 1f
    val newW = (srcW * scale).toInt()
    val newH = (srcH * scale).toInt()
    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, newW, newH, true) else bitmap

    val bos = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, bos)
    return bos.toByteArray()
}