package id.my.matahati.absensi

import android.Manifest
import android.app.Activity
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

// Ganti ke endpoint PHP mobile kamu
private const val FACE_LOGIN_URL =
    "https://absensi.matahati.my.id/face_login_mobile.php"

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

@Composable
fun FaceLoginScreen() {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }

    // Ambil userId dari SessionManager (sama seperti HalamanFaceRegister)
    val storedUserId = session.getUserId()
    val userId = if (storedUserId != -1) {
        storedUserId
    } else {
        (context as? ComponentActivity)
            ?.intent
            ?.getIntExtra("USER_ID", -1) ?: -1
    }

    val scope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf<String?>(null) }
    var statusColor by remember { mutableStateOf(Color.Black) }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ====== TOP BAR: BACK ARROW + TITLE CENTER ======
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
                text = "Face Login",
                fontSize = 22.sp,
                color = Color(0xFFFF6F51),
                fontWeight = FontWeight.Bold
            )
        }

        // ====== PREVIEW KAMERA DENGAN CORNER RADIUS 10dp ======
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

                        val result = loginWithFace(bitmap, userId)

                        isProcessing = false
                        statusColor = if (result.success) Color(0xFF2E7D32) else Color.Red

                        statusText = if (result.success && result.confidence != null) {
                            "${result.message}\nKecocokan: ${"%.1f".format(result.confidence)}%"
                        } else {
                            result.message
                        }

                        if (result.success) {
                            // Delay sebentar lalu tutup halaman (mirip reload di PHP)
                            delay(2500)
                            (context as? Activity)?.finish()
                        }
                    }
                }
            )

            // Kotak overlay di atas preview
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

        // ====== TEKS PERINGATAN DI BAWAH KAMERA ======
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Wajah Harus Penuh (termasuk dagu) Di Dalam Kotak Merah",
            color = Color(0xFFE74C3C),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // ====== TOMBOL AMBIL FOTO ======
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

        // ====== STATUS TEXT ======
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

// ================== CAMERA & NETWORK PART TETAP SAMA ==================

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
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
            val bytes = bos.toByteArray()
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
                        message = "Koneksi gagal. Coba lagi.",
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
