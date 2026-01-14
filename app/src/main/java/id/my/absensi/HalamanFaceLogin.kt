@file:OptIn(ExperimentalGetImage::class)

package id.my.matahati.absensi

import android.Manifest
import android.app.Activity
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
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.ExperimentalAnimationApi

private const val TAG = "FACE_LOGIN"
private const val API_KEY = "MH4T4H4TI_2025_ABSENSI_APP_SECRETx9P2F7Q1L8S3Z0R6W4K2D1M9B7T5"
private const val FACE_LOGIN_URL = "https://absensi.matahati.my.id/user_face_scan_mobile.php"

private val httpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

object FaceLoginDetector {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .build()

    val detector: FaceDetector by lazy {
        FaceDetection.getClient(options)
    }
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

enum class LivenessStep {
    BLINK,
    HEAD_NOD,
    TURN_RIGHT,
    TURN_LEFT,
    SMILE
}

@Composable
fun FaceLoginScreen() {
    var smileDone by remember { mutableStateOf(false) }
    var smilingFrame by remember { mutableStateOf(0) }
    var isSessionStarted by remember { mutableStateOf(false) }
    var eyesClosed by remember { mutableStateOf(false) }
    var blinkDone by remember { mutableStateOf(false) }
    var headUpDone by remember { mutableStateOf(false) }
    var headNodDone by remember { mutableStateOf(false) }
    var headMoveFrame by remember { mutableStateOf(0) }
    var headUpFrame by remember { mutableStateOf(0) }
    var headDownFrame by remember { mutableStateOf(0) }
    var turnRightDone by remember { mutableStateOf(false) }
    var turnLeftDone by remember { mutableStateOf(false) }
    var turnFrame by remember { mutableStateOf(0) }
    var remainingTime by remember { mutableStateOf(0) }
    var isTransitioningStep by remember { mutableStateOf(false) }

    val STEP_TIMEOUT_MS = 3_000L
    val STEP_TRANSITION_DELAY_MS = 300L

    var stepStartTime by remember { mutableStateOf(0L) }
    var livenessSteps by remember { mutableStateOf<List<LivenessStep>>(emptyList()) }

    var currentLivenessIndex by remember { mutableStateOf(0) }
    val currentLivenessStep = livenessSteps.getOrNull(currentLivenessIndex)
    val livenessPassed = currentLivenessIndex >= livenessSteps.size

    val isDoingLiveness = isSessionStarted && !livenessPassed

    fun resetLiveness(autoRestart: Boolean = true) {
        Log.w(TAG, "🔄 RESET LIVENESS")

        blinkDone = false
        eyesClosed = false
        headUpDone = false
        headNodDone = false
        headUpFrame = 0
        headDownFrame = 0
        turnRightDone = false
        turnLeftDone = false
        turnFrame = 0
        smileDone = false
        smilingFrame = 0

        isTransitioningStep = false
        currentLivenessIndex = 0
        stepStartTime = System.currentTimeMillis()

        if (autoRestart) {
            livenessSteps = LivenessStep.values()
                .toList()
                .shuffled()
                .take(2)

            isSessionStarted = true
        }
    }

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

    LaunchedEffect(livenessPassed) {
        if (
            isSessionStarted &&
            livenessPassed &&
            isCameraReady &&
            !isCapturing &&
            !isUploading
        ) {
            delay(400)
            isCapturing = true
            CameraController.capture()
        }
    }

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

    LaunchedEffect(currentLivenessIndex) {
        if (isSessionStarted && !livenessPassed) {
            stepStartTime = System.currentTimeMillis()
            Log.d(TAG, "⏱ Step $currentLivenessIndex started")
        }
    }

    LaunchedEffect(stepStartTime, isSessionStarted) {
        if (!isSessionStarted || stepStartTime == 0L) return@LaunchedEffect

        val timeoutSec = (STEP_TIMEOUT_MS / 1000).toInt()

        while (isSessionStarted && stepStartTime > 0) {
            val elapsed = ((System.currentTimeMillis() - stepStartTime) / 1000).toInt()
            remainingTime = (timeoutSec - elapsed).coerceAtLeast(0)
            delay(200)
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

        //Instruksi liveness
        LivenessInstructionText(
            isSessionStarted = isSessionStarted,
            currentStep = currentLivenessStep,
            headUpDone = headUpDone,
            livenessPassed = livenessPassed
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            CameraPreview(
                onReady = { isCameraReady = true },
                isDoingLiveness = isDoingLiveness,
                isSessionStarted = isSessionStarted,
                currentStepIndex = currentLivenessIndex,
                onFaceFrame = { face, stepIndex ->
                    // ✅ Check stepIndex, bukan currentLivenessIndex
                    if (!isSessionStarted) return@CameraPreview
                    if (stepIndex >= livenessSteps.size) return@CameraPreview
                    val now = System.currentTimeMillis()

                    if (stepStartTime > 0 && now - stepStartTime > STEP_TIMEOUT_MS) {
                        Log.w(TAG, "⏱ STEP TIMEOUT!")

                        statusText = "Waktu habis, silakan ulangi absen"
                        statusColor = Color.Red

                        // reset session
                        statusColor = Color.Red
                        resetLiveness(autoRestart = true)
                        stepStartTime = 0L

                        return@CameraPreview
                    }

                    val step = livenessSteps.getOrNull(stepIndex) ?: return@CameraPreview

                    when (step) {
                        LivenessStep.HEAD_NOD -> {
                            if (headNodDone) return@CameraPreview

                            val pitch = face.headEulerAngleX
                            val absPitch = kotlin.math.abs(pitch)

                            Log.d(TAG, "🎯 HEAD_NOD: pitch=${"%.1f".format(pitch)} up=$headUpDone")

                            // fase naik
                            if (!headUpDone) {
                                if (absPitch > 12f) {
                                    headUpFrame++
                                    if (headUpFrame >= 2) {
                                        headUpDone = true
                                        headUpFrame = 0
                                        Log.d(TAG, "⬆️ HEAD UP OK")
                                    }
                                } else {
                                    headUpFrame = 0
                                }
                                return@CameraPreview
                            }

                            // fase balik
                            // fase balik (event-based, TIDAK pakai frame)
                            if (absPitch < 10f) {
                                Log.d(TAG, "⬇️ HEAD NOD COMPLETE")
                                headNodDone = true

                                if (!isTransitioningStep) {
                                    isTransitioningStep = true
                                    scope.launch {
                                        delay(STEP_TRANSITION_DELAY_MS)
                                        currentLivenessIndex = stepIndex + 1
                                        isTransitioningStep = false
                                    }
                                }

                                // reset state
                                headUpDone = false
                                headUpFrame = 0
                                headDownFrame = 0
                                blinkDone = false
                                eyesClosed = false
                            }
                        }

                        LivenessStep.BLINK -> {
                            if (blinkDone) return@CameraPreview

                            val left = face.leftEyeOpenProbability
                            val right = face.rightEyeOpenProbability

                            if (left == null || right == null) return@CameraPreview

                            Log.d(TAG, "👁️ BLINK: L=${"%.2f".format(left)} R=${"%.2f".format(right)} closed=$eyesClosed done=$blinkDone")

                            if (left < 0.25f && right < 0.25f) {
                                if (!eyesClosed) {
                                    eyesClosed = true
                                    Log.d(TAG, "👁️ EYES CLOSED ✅")
                                }
                                return@CameraPreview
                            }

                            if (eyesClosed && left > 0.6f && right > 0.6f) {
                                Log.d(TAG, "👁️ ✅ BLINK COMPLETE → incrementing")
                                blinkDone = true
                                eyesClosed = false

                                if (!isTransitioningStep) {
                                    isTransitioningStep = true
                                    scope.launch {
                                        delay(STEP_TRANSITION_DELAY_MS)
                                        currentLivenessIndex = stepIndex + 1
                                        isTransitioningStep = false
                                    }
                                }
                            }
                        }

                        LivenessStep.TURN_RIGHT -> {
                            if (turnRightDone) return@CameraPreview

                            val yaw = face.headEulerAngleY
                            Log.d(TAG, "➡️ TURN_RIGHT yaw=${"%.1f".format(yaw)}")

                            if (yaw < -20f) {
                                turnFrame++
                                if (turnFrame >= 2) {
                                    turnRightDone = true
                                    turnFrame = 0

                                    if (!isTransitioningStep) {
                                        isTransitioningStep = true
                                        scope.launch {
                                            delay(STEP_TRANSITION_DELAY_MS)
                                            currentLivenessIndex = stepIndex + 1
                                            isTransitioningStep = false
                                        }
                                    }
                                }
                            }
                        }

                        LivenessStep.TURN_LEFT -> {
                            if (turnLeftDone) return@CameraPreview

                            val yaw = face.headEulerAngleY
                            Log.d(TAG, "⬅️ TURN_LEFT yaw=${"%.1f".format(yaw)}")

                            if (yaw > 20f) {
                                turnFrame++
                                if (turnFrame >= 2) {
                                    turnLeftDone = true
                                    turnFrame = 0

                                    if (!isTransitioningStep) {
                                        isTransitioningStep = true
                                        scope.launch {
                                            delay(STEP_TRANSITION_DELAY_MS)
                                            currentLivenessIndex = stepIndex + 1
                                            isTransitioningStep = false
                                        }
                                    }
                                }
                            }
                        }

                        LivenessStep.SMILE -> {
                            if (smileDone) return@CameraPreview

                            val smileProb = face.smilingProbability
                            if (smileProb == null) return@CameraPreview

                            Log.d(TAG, "😄 SMILE: prob=${"%.2f".format(smileProb)}")

                            if (smileProb > 0.6f) {
                                smilingFrame++
                                if (smilingFrame >= 2) {
                                    smileDone = true
                                    smilingFrame = 0

                                    if (!isTransitioningStep) {
                                        isTransitioningStep = true
                                        scope.launch {
                                            delay(STEP_TRANSITION_DELAY_MS)
                                            currentLivenessIndex = stepIndex + 1
                                            isTransitioningStep = false
                                        }
                                    }
                                }
                            }
                        }

                    }
                },
                onCaptured = { bitmap ->
                    if (bitmap == null) {
                        statusText = "Wajah tidak terdeteksi dengan jelas."
                        statusColor = Color.Red
                        isCapturing = false
                        resetLiveness(autoRestart = true)
                        return@CameraPreview
                    }

                    if (userId <= 0) {
                        statusText = "User tidak valid"
                        statusColor = Color.Red
                        isCapturing = false
                        resetLiveness(autoRestart = true)
                        return@CameraPreview
                    }

                    scope.launch {
                        isUploading = true
                        statusColor = Color.Black

                        val result = uploadFace(bitmap, userId, coordinate, placeName)

                        isUploading = false
                        isCapturing = false
                        statusText = result.message
                        statusColor = if (result.success) Color(0xFF2E7D32) else Color.Red

                        if (result.success) {
                            showSuccessDialog = true
                            isSessionStarted = false
                        } else {
                            resetLiveness(autoRestart = true)
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
            if (currentLivenessStep != null && !livenessPassed) {
//                Text(
//                    text = "⏱ $remainingTime detik",
//                    fontSize = 12.sp,
//                    color = Color.Black,
//                    modifier = Modifier
//                        .align(Alignment.TopCenter)
//                        .padding(top = 8.dp)
//                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("📍 $placeName", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        Button(
            enabled = isCameraReady && !isSessionStarted && !isUploading && !isCapturing,
            onClick = {
                isSessionStarted = true

                livenessSteps = LivenessStep.values()
                    .toList()
                    .shuffled()
                    .take(2)
                Log.d(TAG, "🎲 Liveness steps: $livenessSteps")

                currentLivenessIndex = 0
                stepStartTime = System.currentTimeMillis()

                blinkDone = false
                headNodDone = false
                headUpDone = false
                eyesClosed = false
                headUpFrame = 0
                headDownFrame = 0
                turnRightDone = false
                turnLeftDone = false
                turnFrame = 0
                smileDone = false
                smilingFrame = 0

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
                    isDoingLiveness -> "Ikuti instruksi"
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
    onFaceFrame: (Face, Int) -> Unit,
    onCaptured: (Bitmap?) -> Unit,
    isDoingLiveness: Boolean,
    isSessionStarted: Boolean,
    currentStepIndex: Int
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    // ✅ CRITICAL: Gunakan remember untuk capture perubahan
    val stepIndexState = remember { mutableStateOf(currentStepIndex) }

    // ✅ Update state setiap kali currentStepIndex berubah
    LaunchedEffect(currentStepIndex) {
        Log.e("CAMERA_PREVIEW", "📍 Step index changed: ${stepIndexState.value} → $currentStepIndex")
        stepIndexState.value = currentStepIndex
    }

    DisposableEffect(Unit) {
        Log.d("CAMERA_PREVIEW", "🎬 Started")
        onDispose {
            Log.d("CAMERA_PREVIEW", "🔚 Disposing")
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            Log.d("CAMERA_PREVIEW", "🏭 Creating PreviewView")
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    Log.d("CAMERA_SETUP", "📱 Listener triggered")

                    val cameraProvider = cameraProviderFuture.get()
                    Log.d("CAMERA_SETUP", "✅ Got provider")

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
                                        val face = faces.first()

                                        Handler(Looper.getMainLooper()).post {
                                            val currentIndex = stepIndexState.value
                                            onFaceFrame(face, currentIndex)
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
                                        val rotated = rotateBitmap(bmp, image.imageInfo.rotationDegrees.toFloat())
                                        val inputImage = InputImage.fromBitmap(rotated, 0)

                                        FaceLoginDetector.detector
                                            .process(inputImage)
                                            .addOnSuccessListener { faces ->
                                                if (faces.size != 1) {
                                                    onCaptured(null)
                                                    image.close()
                                                    return@addOnSuccessListener
                                                }

                                                val face = faces.first()

                                                if (!isFaceValidForLogin(face, allowYaw = isDoingLiveness)) {
                                                    onCaptured(null)
                                                    image.close()
                                                    return@addOnSuccessListener
                                                }

                                                val cropped = cropFaceForLogin(rotated, face)
                                                val finalFace = resizeFaceForLogin(cropped)

                                                onCaptured(finalFace)
                                                image.close()
                                            }
                                            .addOnFailureListener {
                                                onCaptured(null)
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

                    Log.d("CAMERA_SETUP", "✅✅✅ BOUND WITH ANALYSIS!")
                    onReady()

                } catch (e: Exception) {
                    Log.e("CAMERA_SETUP", "❌ FAILED", e)
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

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
            .addFormDataPart("facefile", "face.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
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
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&addressdetails=0"
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LivenessInstructionText(
    isSessionStarted: Boolean,
    currentStep: LivenessStep?,
    headUpDone: Boolean,
    livenessPassed: Boolean
) {
    val instructionText = when {
        !isSessionStarted ->
            "Tekan tombol di bawah untuk mulai absensi"

        currentStep == LivenessStep.HEAD_NOD ->
            if (!headUpDone) "⬆️ Gerakkan kepala (atas / bawah)"
            else "⬇️ Kembali ke posisi normal"

        currentStep == LivenessStep.BLINK ->
            "👁️ Kedipkan mata"

        currentStep == LivenessStep.TURN_RIGHT ->
            "➡️ Hadapkan wajah ke kanan"

        currentStep == LivenessStep.TURN_LEFT ->
            "⬅️ Hadapkan wajah ke kiri"

        currentStep == LivenessStep.SMILE ->
            "😄 Senyum ke kamera"

        livenessPassed ->
            "✅ Liveness selesai"

        else -> ""
    }

    val instructionColor = when {
        !isSessionStarted -> Color.Gray
        livenessPassed -> Color(0xFF2E7D32)
        else -> Color(0xFFFF9800)
    }

    AnimatedContent(
        targetState = instructionText,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically { it / 4 }) with
                    (fadeOut(tween(180)) + slideOutVertically { -it / 4 })
        },
        label = "LivenessInstruction"
    ) { text ->
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = instructionColor,
            textAlign = TextAlign.Center
        )
    }
}