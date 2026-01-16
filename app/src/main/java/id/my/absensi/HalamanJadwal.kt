package id.my.matahati.absensi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.matahati.absensi.data.ScheduleViewModel
import id.my.matahati.absensi.data.UserSchedule
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import id.my.matahati.absensi.worker.enqueueScheduleSyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import id.my.matahati.absensi.data.AbsensiViewModel
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp


class HalamanJadwal : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    HalamanJadwalUI()
                }
            }
        }
    }
}

@Composable
fun HalamanJadwalUI(scheduleViewModel: ScheduleViewModel = viewModel()) {
    val absensiViewModel: AbsensiViewModel = viewModel()
    val logs by absensiViewModel.logs.collectAsState()
    val loadingLog by absensiViewModel.loading.collectAsState()
    val errorLog by absensiViewModel.error.collectAsState()

    val schedules by scheduleViewModel.schedules.collectAsState(initial = emptyList())
    val isLoading by scheduleViewModel.loading.collectAsState()
    val error by scheduleViewModel.error.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val session = remember { SessionManager(context.applicationContext) }

    val storedId = session.getUserId()
    val userId = if (storedId != -1) storedId else activity?.intent?.getIntExtra("USER_ID", -1) ?: -1
    var scanState by remember { mutableStateOf(session.getScanState()) }

    var isDescending by remember { mutableStateOf(true) }
    val sortedLogs = if (isDescending) logs.sortedByDescending { it.waktu } else logs.sortedBy { it.waktu }

    val filteredLogs = remember(selectedDate, sortedLogs) {
        if (selectedDate == null) {
            sortedLogs
        } else {
            sortedLogs.filter {
                it.waktu.startsWith(selectedDate.toString())
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            val newState = session.getScanState()
            if (newState != scanState) scanState = newState
            delay(2000L)
        }
    }

    LaunchedEffect(userId, currentMonth) {
        if (userId != -1) {
            val isOnline = id.my.matahati.absensi.utils.NetworkUtils.isOnline(context)
            scheduleViewModel.loadSchedules(userId)
            if (isOnline) enqueueScheduleSyncWorker(context, userId)
        }
    }

    LaunchedEffect(userId) {
        if (userId != -1) absensiViewModel.loadLogs(userId)
    }

    val calendarBackground = Color(0xFFF5F5F5)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(screenHeight * 0.25f)
                .background(color = Color(0xFFB63352))
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val calendarHeight = maxHeight * 0.45f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = screenHeight * 0.05f)
                    .padding(bottom = screenHeight * 0.10f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // === Judul ===
                Text(
                    text = "Jadwal & Shift",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                // === ✅ Kalender selalu muncul meskipun schedule kosong ===
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(calendarHeight),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = calendarBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    CalendarCardContent(
                        currentMonth = currentMonth,
                        onMonthChange = { currentMonth = it },
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        today = today,
                        schedules = schedules // kosong juga tidak masalah
                    )
                }

                // === 🔹 Pesan jika tidak ada shift ===
                if (schedules.isEmpty()) {
                    Text(
                        text = "📭 Belum ada jadwal shift untuk bulan ini",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // === Card Log Absensi ===
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4C4C59))
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Log Absensi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            TextButton(
                                onClick = { selectedDate = null },
                                enabled = selectedDate != null
                            ) {
                                Text(
                                    "Tampilkan Semua",
                                    color = if (selectedDate != null) Color.White else Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFFF9F2))
                                .padding(12.dp)
                        ) {
                            when {
                                loadingLog -> {
                                    Text("⏳ Memuat log absensi...", color = Color.Gray)
                                }

                                errorLog != null -> {
                                    Text("⚠️ $errorLog", color = Color.Red)
                                }

                                filteredLogs.isEmpty() -> {
                                    Text(
                                        text = if (selectedDate != null)
                                            "📭 Belum Ada Riwayat Absensi"
                                        else
                                            "📭 Belum ada log absensi",
                                        color = Color.Gray
                                    )
                                }
                                else -> {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(filteredLogs) { log ->
                                            val bgColor = when (log.typeAbsensi) {
                                                "manual" -> Color(0xFFFFF59D)
                                                "scan" -> Color(0xFFC8D7E6)
                                                "face" -> Color(0xFFBEF2B2)
                                                else -> Color.White
                                            }

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                                elevation = CardDefaults.cardElevation(2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(log.waktu, fontWeight = FontWeight.Bold)
                                                    Text("Jenis: ${log.typeAbsensi?.uppercase() ?: "-"}")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.align(bottomCenter: Alignment) {
    TODO("Not yet implemented")
}

@Composable
private fun CalendarCardContent(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    today: LocalDate,
    schedules: List<UserSchedule>
) {
    val firstOfMonth = currentMonth.atDay(1)
    val firstDayIndex = (firstOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = currentMonth.lengthOfMonth()

    val slots = remember(currentMonth) {
        val list = mutableListOf<LocalDate?>()
        repeat(firstDayIndex) { list.add(null) }
        for (d in 1..daysInMonth) list.add(currentMonth.atDay(d))
        list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 🔹 Header bulan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF4C4C59))
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                    Text("◀", color = Color.White)
                }
                val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("id", "ID"))
                Text(
                    text = "$monthName ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                TextButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Text("▶", color = Color.White)
                }
            }

            val weekNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekNames.forEachIndexed { index, w ->
                    Box(
                        modifier = Modifier.weight(1f).padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val color = if (index == 6) Color.Red else Color.White
                        Text(
                            text = w,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = color
                        )
                    }
                }
            }
        }

        // 🔹 Grid tanggal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(4.dp)
        ) {
            LazyVerticalGrid(columns = GridCells.Fixed(7)) {
                items(slots) { date ->
                    DayCell(
                        date = date,
                        today = today,
                        selected = date == selectedDate,
                        onClick = { onDateSelected(it) },
                        schedules = schedules
                    )
                }
            }
        }
    }
}

fun generateColorFromShift(shiftName: String): Color {
    val hash = abs(shiftName.hashCode())
    val red = (hash % 200) + 30
    val green = ((hash / 200) % 200) + 30
    val blue = ((hash / 40000) % 200) + 30
    return Color(red / 255f, green / 255f, blue / 255f)
}

@Composable
private fun DayCell(
    date: LocalDate?,
    today: LocalDate,
    selected: Boolean,
    onClick: (LocalDate) -> Unit,
    schedules: List<UserSchedule>
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (date == null) {
            Box(modifier = Modifier.fillMaxSize()) {}
        } else {
            val isToday = date == today
            val scheduleForDate = schedules.find { it.dwork == date.toString() }

            val bg = when {
                selected -> Color(0xFF2196F3)
                isToday -> Color(0xFFBDBDBD)
                else -> Color.Transparent
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg, shape = CircleShape)
                    .clickable { onClick(date) },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isSunday = date.dayOfWeek.value == 7
                val dayTextColor = when {
                    isSunday -> Color.Red // 🔹 Angka Minggu merah
                    bg != Color.Transparent -> Color.Black
                    else -> Color.Black
                }

                Text(
                    text = date.dayOfMonth.toString(),
                    color = dayTextColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSunday) FontWeight.Bold else FontWeight.Normal
                    )
                )
                scheduleForDate?.let {
                    val colorDot = generateColorFromShift(it.cschedname)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(colorDot, shape = CircleShape)
                    )
                }
            }
        }
    }
}
