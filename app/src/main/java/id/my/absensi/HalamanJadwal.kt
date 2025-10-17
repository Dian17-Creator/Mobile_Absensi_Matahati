package id.my.matahati.absensi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate
import java.time.YearMonth

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
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val session = remember { SessionManager(context.applicationContext) }

    val storedId = session.getUserId()
    val userId = if (storedId != -1) storedId else activity?.intent?.getIntExtra("USER_ID", -1) ?: -1

    val schedules by scheduleViewModel.schedules.collectAsState(initial = emptyList())
    val isLoading by scheduleViewModel.loading.collectAsState()
    val error by scheduleViewModel.error.collectAsState()
    var scanState by remember { mutableStateOf(session.getScanState()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val newState = session.getScanState()
            if (newState != scanState) {
                scanState = newState
            }
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

    // 🎨 Warna tema
    val CalendarBackground = Color(0xFFF5F5F5)

    // ✅ Ambil tinggi layar dari konfigurasi (bisa digunakan di luar BoxWithConstraints)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // ====== UI UTAMA ======
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ====== Konten dalam BoxWithConstraints (jadwal + kalender) ======
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val calendarHeight = maxHeight * 0.45f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = screenHeight * 0.15f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Jadwal & Shift",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(top = 15.dp)
                )

                when {
                    isLoading -> Text("⏳ Memuat jadwal...", color = Color.Gray)
                    error != null -> Text("⚠️ ${error}", color = Color.Red)
                    schedules.isEmpty() -> Text("📭 Tidak ada jadwal tersedia", color = Color.Gray)
                }

                if (schedules.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(calendarHeight),
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(containerColor = CalendarBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        CalendarCardContent(
                            currentMonth = currentMonth,
                            onMonthChange = { currentMonth = it },
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            today = today,
                            schedules = schedules
                        )
                    }
                }
            }
        }

        // === Shape bawah (sekarang di luar BoxWithConstraints) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(screenHeight * 0.18f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Lapisan pertama (abu kehijauan muda)
                val path1 = Path().apply {
                    moveTo(10f, height * 0.5f)
                    quadraticBezierTo(width * 0.5f, 0f, width, height * 0.3f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(path = path1, color = Color(0xFFDAD4B0))

                // Lapisan kedua (krem muda)
                val path2 = Path().apply {
                    moveTo(0f, height * 0.5f)
                    quadraticBezierTo(width * 0.5f, height * 0.2f, width, height * 0.5f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(path = path2, color = Color(0xFFFAF6D8))
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

                // 🔹 Titik kecil warna dinamis sesuai shift
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
