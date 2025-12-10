package id.my.matahati.absensi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import androidx.compose.material.icons.filled.ArrowBack


private const val TAG_FACE = "FACE_REGISTER"
private val httpClient by lazy { OkHttpClient() }

private const val FACE_UPLOAD_URL =
    "https://absensi.matahati.my.id/user_face_mobile.php"

private const val FACE_STATUS_URL =
    "https://absensi.matahati.my.id/user_face_status_mobile.php"

// 🔴 NEW: endpoint untuk hapus semua foto & reset approval
private const val FACE_RESET_URL =
    "https://absensi.matahati.my.id/user_face_reset_mobile.php"

// status lokal (disinkron dengan SessionManager)
enum class FaceApprovalStatus {
    NONE,       // belum kirim / setelah HR reject / setelah reset
    PENDING,    // sudah upload, menunggu HR
    APPROVED    // sudah disetujui HR
}

// ======================================================
//  ACTIVITY
// ======================================================

class HalamanFaceRegister : ComponentActivity() {

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 10)
        }

        setContent {
            FaceRegisterScreen()
        }
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
}

// ======================================================
//  UI COMPOSABLE
// ======================================================

@Composable
fun FaceRegisterScreen() {

    val poses = listOf(
        "Lihat lurus (netral)",
        "Lihat lurus (senyum)",
        "Miringkan sedikit ke kanan"
    )

    val context = LocalContext.current
    val session = remember { SessionManager(context) }

    // ------------ USER ID ------------
    val storedUserId = session.getUserId()
    val userId = if (storedUserId != -1) {
        storedUserId
    } else {
        (context as? ComponentActivity)
            ?.intent
            ?.getIntExtra("USER_ID", -1) ?: -1
    }

    Log.d(TAG_FACE, "FaceRegisterScreen started, userId = $userId")

    // Map bitmap pose
    val poseBitmaps: SnapshotStateMap<Int, Bitmap> =
        remember { mutableStateMapOf<Int, Bitmap>() }

    // ------------ STATUS FACE DARI SESSION ------------
    var faceStatus by remember {
        mutableStateOf(
            when (session.getFaceStatus()) {
                "PENDING" -> FaceApprovalStatus.PENDING
                "APPROVED" -> FaceApprovalStatus.APPROVED
                else -> FaceApprovalStatus.NONE
            }
        )
    }

    // 🚀 Load status + foto dari server saat screen dibuka
    LaunchedEffect(userId) {
        if (userId == -1) return@LaunchedEffect

        val (serverStatus, serverBitmaps) = fetchFaceStatusFromServer(userId)

        Log.d(TAG_FACE, "Server status = $serverStatus, bitmaps = ${serverBitmaps.size}")

        // Update status & simpan ke session
        faceStatus = serverStatus
        when (serverStatus) {
            FaceApprovalStatus.PENDING -> session.setFaceStatus("PENDING")
            FaceApprovalStatus.APPROVED -> session.setFaceStatus("APPROVED")
            FaceApprovalStatus.NONE -> session.setFaceStatus(null)   // clear -> boleh retake
        }

        // Isi ulang preview dari server (kalau ada)
        poseBitmaps.clear()
        serverBitmaps.forEachIndexed { index, bmp ->
            poseBitmaps[index] = bmp
        }
    }

    // pose berikutnya yang harus diambil (0..2) atau null kalau sudah lengkap
    val nextPoseIndex = (0 until poses.size).firstOrNull { !poseBitmaps.containsKey(it) }

    val scope = rememberCoroutineScope()
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // 🔴 NEW: flag reset
    var isResetting by remember { mutableStateOf(false) }

    // boleh ambil ulang via kamera hanya kalau status NONE
    val canRetake = faceStatus == FaceApprovalStatus.NONE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ====== TOP BAR: ARROW + TITLE ======
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { (context as? android.app.Activity)?.finish() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color(0xFFFF6F51)
                )
            }

            Text(
                text = "Registrasi Wajah",
                fontSize = 22.sp,
                color = Color(0xFFFF6F51),
                fontWeight = FontWeight.Bold
            )
        }

        Text("Ikuti instruksi berikut untuk hasil yang lebih akurat")
        Spacer(modifier = Modifier.height(12.dp))

        val poseText = if (canRetake) {
            nextPoseIndex?.let { poses[it] } ?: "Semua pose sudah lengkap ✔"
        } else {
            "Semua pose sudah lengkap ✔"
        }

        Text(
            text = "Pose: $poseText",
            fontSize = 18.sp,
            color = Color.DarkGray
        )

        // ======= KAMERA (hanya kalau boleh retake dan belum 3 foto) =======
        if (canRetake && nextPoseIndex != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                CameraPreviewRegister(
                    onImageCaptured = { bitmap ->
                        Log.d(TAG_FACE, "Bitmap captured for poseIndex=$nextPoseIndex")
                        poseBitmaps[nextPoseIndex] = bitmap
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(3f / 4f)
                        .border(2.dp, Color(0xFFFF6F51), RoundedCornerShape(10.dp))
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    Log.d(TAG_FACE, "Capture button clicked")
                    CameraRegisterController.capture()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F51))
            ) {
                Text("Ambil Foto")
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(poses.indices.toList()) { index ->
                    val bmp = poseBitmaps[index]
                    if (bmp != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(100.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .align(Alignment.BottomStart)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color.Gray)
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = poses[index],
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                                if (canRetake) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = 4.dp)
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = CircleShape,
                                                clip = false
                                            )
                                            .background(
                                                color = Color.White,
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                Log.d(TAG_FACE, "Delete photo at index=$index")
                                                poseBitmaps.remove(index)
                                                uploadStatus = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus foto",
                                            tint = Color.Red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = when (index) {
                                    0 -> "Netral"
                                    1 -> "Senyum"
                                    2 -> "Miring"
                                    else -> "Pose ${index + 1}"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ======= TEKS STATUS =======
        if (faceStatus != FaceApprovalStatus.NONE || poseBitmaps.size == poses.size) {
            when (faceStatus) {
                FaceApprovalStatus.NONE -> {
                    if (poseBitmaps.size == poses.size) {
                        Text(
                            text = "3 foto selesai.\nSilakan klik \"Simpan Semua Foto\".",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                FaceApprovalStatus.PENDING -> {
                    Text(
                        text = "3 foto selesai\nMenunggu persetujuan HR...",
                        fontSize = 13.sp,
                        color = Color(0xFFFF9800),
                        textAlign = TextAlign.Center
                    )
                }

                FaceApprovalStatus.APPROVED -> {
                    Text(
                        text = "3 foto selesai\nFoto telah disetujui HR.",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔴 NEW: TOMBOL "AMBIL ULANG FOTO WAJAH" (hanya kalau APPROVED)
        if (faceStatus == FaceApprovalStatus.APPROVED) {
            OutlinedButton(
                onClick = {
                    if (userId == -1) {
                        uploadStatus = "User tidak terdeteksi, silakan login ulang."
                        Log.e(TAG_FACE, "USER ID INVALID (-1) saat reset")
                        return@OutlinedButton
                    }

                    scope.launch {
                        isResetting = true
                        uploadStatus = "Menghapus foto lama..."

                        val ok = resetFaceOnServer(userId)

                        isResetting = false
                        if (ok) {
                            // bersihkan preview & status, kembali ke mode ambil ulang
                            poseBitmaps.clear()
                            faceStatus = FaceApprovalStatus.NONE
                            session.setFaceStatus(null)

                            uploadStatus =
                                "Foto lama dihapus. Silakan ambil ulang 3 foto wajah."
                        } else {
                            uploadStatus =
                                "Gagal menghapus foto lama. Coba lagi."
                        }
                    }
                },
                enabled = !isUploading && !isResetting,
                modifier = Modifier
                    .fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFC62828)
                )
            ) {
                Text(if (isResetting) "Menghapus..." else "Ambil Ulang Foto Wajah")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ======= TOMBOL SIMPAN =======
        if (poseBitmaps.isNotEmpty()) {
            Button(
                onClick = {
                    Log.d(TAG_FACE, "Submit clicked")
                    Log.d(TAG_FACE, "UserId = $userId")
                    Log.d(TAG_FACE, "Total images in map = ${poseBitmaps.size}")

                    if (userId == -1) {
                        uploadStatus = "User tidak terdeteksi, silakan login ulang."
                        Log.e(TAG_FACE, "USER ID INVALID (-1)")
                        return@Button
                    }

                    scope.launch {
                        isUploading = true
                        uploadStatus = "Mengunggah foto..."

                        val bitmaps = (0 until poses.size).mapNotNull { poseBitmaps[it] }
                        Log.d(TAG_FACE, "Bitmaps prepared for upload = ${bitmaps.size}")

                        val success = uploadAllFaceImages(bitmaps, userId)

                        isUploading = false
                        if (success) {
                            faceStatus = FaceApprovalStatus.PENDING
                            session.setFaceStatus("PENDING")
                            uploadStatus =
                                "Semua foto berhasil diunggah. Menunggu persetujuan HR."
                        } else {
                            uploadStatus = "Gagal mengunggah foto. Coba lagi."
                        }

                        Log.d(TAG_FACE, "UPLOAD RESULT = $success")
                    }
                },
                enabled = canRetake && poseBitmaps.size == poses.size && !isUploading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF008C4A),
                    disabledContainerColor = Color(0xFF8BC34A)
                )
            ) {
                Text(if (isUploading) "Mengunggah..." else "Simpan Semua Foto")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Tombol baru di bawahnya
            Button(
                onClick = {
                    val intent = Intent(context, HalamanFaceLogin::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4C4C59),
                    contentColor = Color.White
                )
            ) {
                Text("Face Login")
            }
        }

        uploadStatus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, fontSize = 13.sp, color = Color.DarkGray)
        }
    }
}

// ======================================================
//  CAMERA PREVIEW + CAPTURE
// ======================================================

object CameraRegisterController {
    var capture: () -> Unit = {}
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewRegister(
    onImageCaptured: (Bitmap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val currentOnImageCaptured by rememberUpdatedState(onImageCaptured)

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageCapture = ImageCapture.Builder().build()

            Log.d(TAG_FACE, "Binding camera to lifecycle")
            CameraRegisterController.capture = {
                Log.d(TAG_FACE, "takePicture() called")
                imageCapture.takePicture(
                    executor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            try {
                                Log.d(TAG_FACE, "Capture success, imageProxy received")

                                val rawBitmap = imageProxyToBitmap(image)

                                Log.d(
                                    TAG_FACE,
                                    "Raw bitmap size = ${rawBitmap.width} x ${rawBitmap.height}"
                                )

                                val rotated = rotateBitmap(
                                    rawBitmap,
                                    image.imageInfo.rotationDegrees.toFloat()
                                )

                                val maxWidth = 600
                                val ratio = maxWidth.toFloat() / rotated.width.toFloat()
                                val thumbWidth = maxWidth
                                val thumbHeight = (rotated.height * ratio).toInt()

                                val thumbnail = Bitmap.createScaledBitmap(
                                    rotated,
                                    thumbWidth,
                                    thumbHeight,
                                    true
                                )

                                Log.d(
                                    TAG_FACE,
                                    "Thumbnail ready = ${thumbnail.width} x ${thumbnail.height}"
                                )

                                currentOnImageCaptured(thumbnail)

                            } catch (e: Exception) {
                                Log.e(TAG_FACE, "Error in capture processing", e)
                            } finally {
                                image.close()
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG_FACE, "ImageCapture onError", exception)
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
            Log.d(TAG_FACE, "Camera bound to lifecycle")

        }, ContextCompat.getMainExecutor(ctx))

        previewView
    })
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply {
        postRotate(degrees)
        postScale(-1f, 1f, src.width / 2f, src.height / 2f) // unmirror
    }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

suspend fun downloadBitmap(url: String): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val stream = resp.body?.byteStream() ?: return@withContext null
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG_FACE, "downloadBitmap error: $url", e)
            null
        }
    }

/**
 * Ambil status & url foto dari server
 */
suspend fun fetchFaceStatusFromServer(userId: Int): Pair<FaceApprovalStatus, List<Bitmap>> =
    withContext(Dispatchers.IO) {
        try {
            val jsonReq = """
                { "userId": $userId }
            """.trimIndent()

            val body = jsonReq.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(FACE_STATUS_URL)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                Log.d(TAG_FACE, "STATUS HTTP CODE = ${resp.code}")
                Log.d(TAG_FACE, "STATUS RESPONSE = $respBody")

                if (!resp.isSuccessful || respBody.isEmpty()) {
                    return@withContext FaceApprovalStatus.NONE to emptyList()
                }

                val obj = JSONObject(respBody)
                if (!obj.optBoolean("success", false)) {
                    return@withContext FaceApprovalStatus.NONE to emptyList()
                }

                val statusStr = obj.optString("status", "none").lowercase()
                val status = when (statusStr) {
                    "approved" -> FaceApprovalStatus.APPROVED
                    "pending" -> FaceApprovalStatus.PENDING
                    else -> FaceApprovalStatus.NONE
                }

                val facesArr = obj.optJSONArray("faces")
                val bitmaps = mutableListOf<Bitmap>()
                if (facesArr != null) {
                    val count = minOf(3, facesArr.length())
                    for (i in 0 until count) {
                        val fObj = facesArr.optJSONObject(i) ?: continue
                        val url = fObj.optString("url")
                        if (url.isNotBlank()) {
                            downloadBitmap(url)?.let { bitmaps.add(it) }
                        }
                    }
                }

                return@withContext status to bitmaps
            }
        } catch (e: Exception) {
            Log.e(TAG_FACE, "fetchFaceStatusFromServer error", e)
            FaceApprovalStatus.NONE to emptyList()
        }
    }

// 🔴 NEW: reset semua foto + status di server
suspend fun resetFaceOnServer(userId: Int): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val jsonReq = """
                { "userId": $userId }
            """.trimIndent()

            val body = jsonReq.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(FACE_RESET_URL)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                Log.d(TAG_FACE, "RESET HTTP CODE = ${resp.code}")
                Log.d(TAG_FACE, "RESET RESPONSE = $respBody")

                if (!resp.isSuccessful || respBody.isEmpty()) return@withContext false

                val obj = JSONObject(respBody)
                obj.optBoolean("success", false)
            }
        } catch (e: Exception) {
            Log.e(TAG_FACE, "resetFaceOnServer error", e)
            false
        }
    }

suspend fun uploadFaceImage(bitmap: Bitmap, userId: Int): Boolean =
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

            Log.d(TAG_FACE, "POST to $FACE_UPLOAD_URL")
            Log.d(TAG_FACE, "JSON length = ${json.length}")

            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FACE_UPLOAD_URL)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val code = resp.code
                val respBody = resp.body?.string()
                Log.d(TAG_FACE, "UPLOAD HTTP CODE = $code")
                Log.d(TAG_FACE, "UPLOAD RESPONSE = $respBody")

                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG_FACE, "Exception when uploading face", e)
            false
        }
    }

suspend fun uploadAllFaceImages(bitmaps: List<Bitmap>, userId: Int): Boolean {
    Log.d(TAG_FACE, "Uploading ${bitmaps.size} images (sequential)")
    for ((idx, bmp) in bitmaps.withIndex()) {
        Log.d(TAG_FACE, "Uploading image ${idx + 1}")
        val ok = uploadFaceImage(bmp, userId)
        if (!ok) {
            Log.e(TAG_FACE, "FAILED at image ${idx + 1}")
            return false
        }
    }
    return true
}
