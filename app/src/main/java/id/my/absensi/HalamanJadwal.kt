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
import androidx.compose.ui.draw.clip
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

    val userId = activity?.intent?.getIntExtra("USER_ID", -1) ?: -1
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

    val CalendarBackground = Color(0xFFF5F5F5)
    val primaryColor = Color(0xFFFF6F51)

    // 🔹 Gunakan Box agar FAB bisa di posisi bawah kanan
    Box(modifier = Modifier.fillMaxSize()) {

        // ======= Bagian utama isi layar =======
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

            // ✅ Card Kalender
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(365.dp)
                    .padding(0.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = CalendarBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // 🔸 Bagian header (bulan + nama hari)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4C4C59))
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                                Text("◀", color = Color.White)
                            }
                            val monthName =
                                currentMonth.month.getDisplayName(TextStyle.FULL, Locale("id", "ID"))
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

                        // 🔹 Baris hari dengan warna kontras di atas
                        val weekNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for ((index, w) in weekNames.withIndex()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val color =
                                        if (index == 6) Color.Red else Color.White // Minggu tetap merah
                                    Text(
                                        text = w,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = color
                                    )
                                }
                            }
                        }
                    }

                    // 🔸 Bagian isi tanggal
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5)) // background bawah tetap terang
                            .padding(8.dp)
                    ) {
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ✅ Card Shift
            val selectedShift = selectedDate?.let { sel ->
                schedules.find { it.dwork == sel.toString() }
            }
            val shiftColor = selectedShift?.let { generateColorFromShift(it.cschedname) } ?: CalendarBackground

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = shiftColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Shift",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selectedShift != null) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedShift != null) {
                        Text(
                            text = "${selectedShift.cschedname.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} (${selectedShift.dstart} - ${selectedShift.dend})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Belum ada shift untuk tanggal ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
            }

            // 🔹 Tambahkan Spacer di sini agar ada jarak antara card dan tombol
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = Intent(context, HalamanIzin::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Izin Tidak Masuk")
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
