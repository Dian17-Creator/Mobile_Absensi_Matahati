package id.my.matahati.absensi

import android.os.Bundle
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.matahati.absensi.data.ScheduleViewModel
import androidx.compose.ui.text.font.FontWeight
import id.my.matahati.absensi.data.UserSchedule
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

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
    val sessionManager = remember { SessionManager(context) }
    val userId = sessionManager.getUserId()

    val schedules by scheduleViewModel.schedules.collectAsState(initial = emptyList())

    // Load sesuai user login
    LaunchedEffect(userId) {
        if (userId != -1) {
            scheduleViewModel.loadSchedules(userId)
        }
    }

    // Build slot tanggal
    val firstOfMonth = currentMonth.atDay(1)
    val firstDayIndex = (firstOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val slots = remember(currentMonth) {
        val list = mutableListOf<LocalDate?>()
        repeat(firstDayIndex) { list.add(null) }
        for (d in 1..daysInMonth) list.add(currentMonth.atDay(d))
        list
    }

    val orange = Color(0xFFFF6F51)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header judul + tombol kembali
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { if (context is ComponentActivity) context.finish() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.Black
                )
            }

            Text(
                text = "Jadwal & Shift",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF333333),
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        // ✅ Card Kalender dengan background oranye
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = orange),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                // Header bulan
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
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Text("▶", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hari
                val weekNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (w in weekNames) {
                        Box(modifier = Modifier.weight(1f).padding(4.dp), contentAlignment = Alignment.Center) {
                            Text(text = w, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Grid tanggal
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                ) {
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

        Spacer(modifier = Modifier.height(20.dp))

        // ✅ Card Shift dengan background oranye
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = orange),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Shift",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                val note = selectedDate?.let { sel ->
                    schedules.find { it.dwork == sel.toString() }
                }

                if (note != null) {
                    Text(
                        text = "${note.cschedname.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} (${note.dstart} - ${note.dend})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Belum ada shift untuk tanggal ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
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
            val hasSchedule = schedules.any { it.dwork == date.toString() }

            val bg = when {
                selected -> Color(0xFF2196F3)
                isToday -> Color(0xF61616161) // abu-abu sedang
                else -> Color.Transparent
            }
            val txtColor = if (bg != Color.Transparent) Color.White else Color.Black

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg, shape = CircleShape)
                    .clickable { onClick(date) },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = txtColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (hasSchedule) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.White, shape = CircleShape)
                    )
                }
            }
        }
    }
}
