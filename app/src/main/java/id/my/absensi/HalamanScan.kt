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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.Locale
import id.my.matahati.absensi.data.ScheduleViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.jvm.java
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import id.my.matahati.absensi.RuntimeSession.userId
import id.my.matahati.absensi.data.RetrofitClient

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
) {
    var hasPendingApproval by remember { mutableStateOf(false) }
    var isSessionStarted by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    var statusText by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color.Black) }

    var coordinate by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("Mengambil lokasi...") }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) return@LaunchedEffect

        val activity = context as Activity

        val hasLocationPermission =
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            placeName = "Lokasi tidak diizinkan"
            return@LaunchedEffect
        }

        val fused = LocationServices.getFusedLocationProviderClient(activity)
        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    coordinate = "${loc.latitude},${loc.longitude}"
                    CoroutineScope(Dispatchers.IO).launch {
                        val place = reverseGeocode(loc.latitude, loc.longitude)
                        withContext(Dispatchers.Main) {
                            placeName = place.ifEmpty { coordinate }
                        }
                    }
                } else {
                    placeName = "Lokasi tidak tersedia"
                }
            }
            .addOnFailureListener {
                placeName = "Gagal mengambil lokasi"
            }
    }

    val session = SessionManager(context)

    val isCaptainOrAbove = remember {
        session.isCaptainOrAbove()
    }

    val activity = context as? ComponentActivity

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val primaryColor = Color(0xFFB63352)
    val backColor = Color(0xFFFFF5F5)

    val storedUserId = session.getUserId()
    val userId = if (storedUserId != -1) storedUserId else activity?.intent?.getIntExtra("USER_ID", -1) ?: -1
    val userName = if (storedUserId != -1) session.getUser()["name"]?.toString() ?: "" else activity?.intent?.getStringExtra("USER_NAME") ?: ""
    val userEmail = if (storedUserId != -1) session.getUser()["email"]?.toString() ?: "" else activity?.intent?.getStringExtra("USER_EMAIL") ?: ""

    //Untuk pending absen manual/izin
    LaunchedEffect(isCaptainOrAbove) {
        if (!isCaptainOrAbove) return@LaunchedEffect

        try {
            val manual =
                RetrofitClient.instance.getApprovalList("mscan_manual", userId)
            val izin =
                RetrofitClient.instance.getApprovalList("mrequest", userId)

            hasPendingApproval =
                (manual.body()?.data?.isNotEmpty() == true) ||
                        (izin.body()?.data?.isNotEmpty() == true)

        } catch (e: Exception) {
            hasPendingApproval = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // 🔥 RELOAD SAAT BALIK KE HOME
                CoroutineScope(Dispatchers.Main).launch {
                    hasPendingApproval =
                        checkPendingApproval(userId, isCaptainOrAbove)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 🌈 UI utama
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backColor)
    ) {
        // 🔸 Background atas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .clip(BottomCurveShape(curveHeight = 50f))
                .background(primaryColor)
                .align(Alignment.TopCenter)
        )

        // 🔸 Konten utama
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
//                .background(backColor)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Card Waktu
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4C4C59)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    CardWaktu()
                }
            }

            //Card Shift
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    CardShift(userId = userId)
                }
            }

            //Box Face Absensi
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(16.dp)),
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
                            if (bitmap == null || userId <= 0) {
                                statusText = "Wajah tidak terdeteksi"
                                statusColor = Color.Red
                                isCapturing = false
                                isSessionStarted = false
                                return@CameraPreview
                            }

                            CoroutineScope(Dispatchers.Main).launch {
                                isUploading = true
                                val result = uploadFace(bitmap, userId, coordinate, placeName)
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
            }

            //Lokasi text
            item {
                Text("📍 $placeName", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }

            //Button submit
            item {
                Button(
                    enabled = isCameraReady && !isUploading && !isCapturing,
                    onClick = {
                        isSessionStarted = true
                        statusText = ""
                        statusColor = Color.Black
                    },
                    modifier = Modifier.fillMaxWidth().height(45.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    )
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
                                Text("Data Absen Sudah Disimpan", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                Button(
                                    onClick = {
                                        showSuccessDialog = false
                                        statusText = ""
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

            //row button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4), // ✅ 1 BARIS = 4 MENU
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp), // 🔥 nempel atas
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        userScrollEnabled = false // ❌ grid diam, tidak scroll
                    ) {

                        item {
                            UserActionItem(
                                icon = Icons.Default.Event,
                                label = "Izin"
                            ) {
                                val intent = Intent(context, HalamanIzin::class.java).apply {
                                    putExtra("USER_ID", userId)
                                    putExtra("USER_NAME", userName)
                                    putExtra("USER_EMAIL", userEmail)
                                }
                                context.startActivity(intent)
                            }
                        }

                        item {
                            UserActionItem(
                                icon = Icons.Default.Edit,
                                label = "Manual"
                            ) {
                                context.startActivity(Intent(context, HalamanManual::class.java))
                            }
                        }

                        item {
                            UserActionItem(
                                icon = Icons.Default.List,
                                label = "Aktivitas"
                            ) {
                                context.startActivity(Intent(context, HalamanAktivitas::class.java))
                            }
                        }

                        item {
                            UserActionItem(
                                icon = Icons.Default.Face,
                                label = "Face Reg"
                            ) {
                                val intent = Intent(context, HalamanFaceRegister::class.java).apply {
                                    putExtra("USER_ID", userId)
                                    putExtra("USER_NAME", userName)
                                    putExtra("USER_EMAIL", userEmail)
                                }
                                context.startActivity(intent)
                            }
                        }

                        item {
                            UserActionItem(
                                icon = Icons.Default.Close,
                                label = "Lupa Absen"
                            ) {
                                context.startActivity(Intent(context, HalamanForgot::class.java))
                            }
                        }

                        item {
                            UserActionItem(
                                icon = Icons.Default.MonetizationOn,
                                label = "Gaji"
                            ) {
                                context.startActivity(Intent(context, HalamanGaji::class.java))
                            }
                        }

                        if (isCaptainOrAbove) {
                            item {
                                UserActionItem(
                                    icon = Icons.Default.Verified,
                                    label = "Approval",
                                    showBadge = hasPendingApproval
                                ) {
                                    context.startActivity(Intent(context, HalamanApproval::class.java))
                                }
                            }
                        }

                        item {
                            UserActionItem(
                                icon = Icons.Default.Logout,
                                label = "Logout",
                                iconColor = Color(0xFFD32F2F)
                            ) {
                                LogoutHelper.logout(context)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* 🔹 CARD WAKTU (real-time, versi ringkas satu baris) */
@Composable
fun CardWaktu() {
    var waktuSekarang by remember { mutableStateOf("") }
    var tanggalSekarang by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalDateTime.now()
            val formatterJam = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            val formatterTgl = java.time.format.DateTimeFormatter.ofPattern(
                "EEE, dd MMM yyyy", // disingkat: Rabu -> Rab, Oktober -> Okt
                Locale("id", "ID")
            )
            waktuSekarang = now.format(formatterJam)
            tanggalSekarang = now.format(formatterTgl)
            delay(1000L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$waktuSekarang • $tanggalSekarang",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White,
            maxLines = 1
        )
    }
}

/* 🔹 CARD SHIFT (warna & periode dinamis) */
@Composable
fun CardShift(
    userId: Int,
    scheduleViewModel: ScheduleViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val schedules by scheduleViewModel.schedules.collectAsState()
    val today = LocalDate.now()

    LaunchedEffect(userId) {
        if (userId != -1) {
            scheduleViewModel.loadSchedules(userId)
        }
    }

    // ================= DATA HARI INI =================
    val todayRows = schedules.filter { it.dwork == today.toString() }

    val shiftNameToday = todayRows.firstOrNull()?.cschedname

    val shiftColor = shiftNameToday
        ?.let { generateColorFromShift(it) }
        ?: Color(0xFFF5F5F5)

    // ================= PERIODE SHIFT =================
    val sameShiftDates = schedules
        .filter { it.cschedname == shiftNameToday }
        .mapNotNull { runCatching { LocalDate.parse(it.dwork) }.getOrNull() }
        .sorted()

    val runs = mutableListOf<MutableList<LocalDate>>()
    for (d in sameShiftDates) {
        if (runs.isEmpty()) {
            runs.add(mutableListOf(d))
        } else {
            val lastRun = runs.last()
            if (lastRun.last().plusDays(1) == d) lastRun.add(d)
            else runs.add(mutableListOf(d))
        }
    }

    val containingRun = runs.find { it.contains(today) }
    val minDate = containingRun?.minOrNull()
    val maxDate = containingRun?.maxOrNull()

    val formatterShort =
        DateTimeFormatter.ofPattern("dd MMM", Locale("id", "ID"))

    val periodeText =
        if (minDate != null && maxDate != null)
            "${minDate.format(formatterShort)} - ${maxDate.format(formatterShort)}"
        else null

    // ================= UI =================
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = shiftColor),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

//            Text(
//                text = "Shift Hari Ini",
//                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
//                color = if (todayRows.isNotEmpty()) Color.White else Color.Black
//            )

            Spacer(modifier = Modifier.height(4.dp))

            if (todayRows.isNotEmpty()) {

                // 🔹 Baris 1: Judul + Nama Shift
                Text(
                    text = "Shift Hari Ini : ${
                        shiftNameToday!!.replaceFirstChar {
                            it.titlecase(Locale("id", "ID"))
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 🔥 Jam shift (support split)
                val timeText = todayRows.joinToString(" | ") {
                    "${it.dstart.substring(0, 5)} - ${it.dend.substring(0, 5)}"
                }

                // 🔹 Baris 2: Jam + Periode
                Text(
                    text = buildString {
                        append("($timeText)")
                        periodeText?.let {
                            append(" $it ${today.year}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White,
                    lineHeight = 14.sp,
                    maxLines = 2
                )

            } else {
                Text(
                    text = "Belum ada shift untuk hari ini.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun UserActionItem(
    icon: ImageVector,
    label: String,
    iconColor: Color = Color(0xFFB63352),
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {

        Box( // 🔥 PARENT UTAMA
            modifier = Modifier.size(64.dp)
        ) {

            Card(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                onClick = onClick
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.Red, shape = CircleShape)
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-5).dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFB63352),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}


class BottomCurveShape(
    private val curveHeight: Float = 120f // tinggi lengkungan
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ) = androidx.compose.ui.graphics.Outline.Generic(
        Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height - curveHeight)

            // ⭕ Lengkungan setengah lingkaran
            quadraticBezierTo(
                size.width / 2,
                size.height + curveHeight,
                size.width,
                size.height - curveHeight
            )

            lineTo(size.width, 0f)
            close()
        }
    )
}

suspend fun checkPendingApproval(
    userId: Int,
    isCaptainOrAbove: Boolean
): Boolean {
    if (!isCaptainOrAbove) return false

    return try {
        val manual =
            RetrofitClient.instance.getApprovalList("mscan_manual", userId)
        val izin =
            RetrofitClient.instance.getApprovalList("mrequest", userId)

        (manual.body()?.data?.isNotEmpty() == true) ||
                (izin.body()?.data?.isNotEmpty() == true)

    } catch (e: Exception) {
        false
    }
}