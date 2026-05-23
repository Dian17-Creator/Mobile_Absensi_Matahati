package id.my.matahati.absensi

import android.app.Activity
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.matahati.absensi.data.RetrofitClient
import id.my.matahati.absensi.data.AktivitasResponse
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

class HalamanAktivitas : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HalamanAktivitasScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalamanAktivitasScreen() {

    val tabs = listOf("Absen Manual", "Izin")

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    var aktivitasList by remember { mutableStateOf<List<AktivitasResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val userId = sessionManager.getUserId()

    // Ambil page aktif dari pager
    val selectedTab = pagerState.currentPage

    // Fetch data ketika page berubah
    LaunchedEffect(selectedTab) {
        isLoading = true
        errorMessage = null

        try {
            val type = if (selectedTab == 0) "mscan_manual" else "mrequest"

            Log.d("AKTIVITAS", "Fetching $type untuk userId=$userId")

            val response = RetrofitClient.instance.getAktivitas(type, userId)

            if (response.isSuccessful && response.body()?.success == true) {
                aktivitasList = response.body()?.data ?: emptyList()
            } else {
                errorMessage = "Gagal memuat data (${response.code()})"
            }

        } catch (e: Exception) {
            errorMessage = "Koneksi gagal: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .background(Color(0xFFB63352)),
            contentAlignment = Alignment.BottomCenter
        ) {

            IconButton(
                onClick = { (context as Activity).finish() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }

            Text(
                text = "AKTIVITAS",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 15.dp)
            )
        }

        // TAB
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFB63352),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->

                Tab(
                    selected = selectedTab == index,

                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },

                    text = {
                        Text(
                            title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
            }
        }

        // PAGER (BISA DI SWIPE)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->

            when {
                isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFB63352))
                    }
                }

                errorMessage != null -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(errorMessage ?: "Terjadi kesalahan", color = Color.Red)
                    }
                }

                aktivitasList.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada data", color = Color.Gray)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(aktivitasList) { item ->
                            AktivitasCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AktivitasCard(item: AktivitasResponse) {
    // Tentukan status gabungan (final)
    val finalStatus = when {
        item.cstatus.equals("rejected", true) || item.chrdstat.equals("rejected", true) -> "rejected"
        item.cstatus.equals("approved", true) || item.chrdstat.equals("approved", true) -> "approved"
        else -> "pending"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Kolom teks di kiri
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(end = 90.dp) // ✅ jarak aman dari badge kanan
            ) {
                Text(item.cplacename ?: "-", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.creason ?: "-", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.tanggal, fontSize = 11.sp, color = Color.Gray)
            }

            // Badge status di kanan atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp) // ✅ beri sedikit jarak dari tepi kanan
            ) {
                StatusButton(finalStatus)
            }
        }
    }
}

@Composable
fun StatusButton(status: String) {
    val color = when (status.lowercase()) {
        "pending" -> Color(0xFFFFC107)
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        "by_system" -> Color(0xFF2196F3)
        else -> Color.LightGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}