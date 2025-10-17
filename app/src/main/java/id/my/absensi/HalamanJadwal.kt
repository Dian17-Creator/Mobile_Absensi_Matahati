package id.my.matahati.absensi

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.matahati.absensi.data.ScheduleViewModel
import androidx.compose.ui.text.font.FontWeight
import id.my.matahati.absensi.data.UserSchedule
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import id.my.matahati.absensi.worker.enqueueScheduleSyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

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
    var scanState by remember { mutableStateOf(session.getScanState()) }

    LaunchedEffect(Unit) {
        // gunakan loop yang berhenti saat composition dibuang
        while (isActive) {
            val newState = session.getScanState()
            if (newState != scanState) {
                scanState = newState
            }
            delay(2000L) // 2 detik
        }
    }

    LaunchedEffect(userId, currentMonth, scanState) {
        if (userId != -1) {
            scheduleViewModel.loadSchedules(userId)
            enqueueScheduleSyncWorker(context, userId) // 🔁 sync otomatis
        }
    }

    val firstOfMonth = currentMonth.atDay(1)
    val firstDayIndex = (firstOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val slots = remember(currentMonth) {
        val list = mutableListOf<LocalDate?>()
        repeat(firstDayIndex) { list.add(null) }
        for (d in 1..daysInMonth) list.add(currentMonth.atDay(d))
        list
    }

    val CalendarBackground = Color(0xFFF5F5F5)
    val primaryColor = Color(0xFFFF6F51)

    // ✅ Layout adaptif untuk semua ukuran layar
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        val headerHeight = screenHeight * 0.08f
        val calendarHeight = screenHeight * 0.45f
        val shiftCardHeight = screenHeight * 0.18f
        val buttonHeight = screenHeight * 0.08f
        val spacing = screenHeight * 0.02f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp), // beri jarak atas–bawah ringan
            verticalArrangement = Arrangement.spacedBy(13.dp), // jarak antar elemen 10dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Header judul + tombol kembali
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Jadwal & Shift",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(top = 15.dp)
                )
            }

            // 🔹 Card Kalender (proporsional)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(calendarHeight),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = CalendarBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header bulan
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
                            TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                                Text("◀", color = Color.White)
                            }
                            val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("id", "ID"))
                            Text(
                                text = "$monthName ${currentMonth.year}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                                Text("▶", color = Color.White)
                            }
                        }

                        val weekNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for ((index, w) in weekNames.withIndex()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp),
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

                    // Isi tanggal
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
                                    onClick = { selectedDate = it },
                                    schedules = schedules
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🔹 Fungsi tambahan untuk warna dinamis berdasarkan nama shift
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
