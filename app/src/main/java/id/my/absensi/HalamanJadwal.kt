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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import id.my.matahati.absensi.data.UserAgendaViewModel
import id.my.matahati.absensi.RuntimeSession.userId
import id.my.matahati.absensi.data.UserAgenda
import id.my.matahati.absensi.data.UserContract
import id.my.matahati.absensi.data.UserContractViewModel
import id.my.matahati.absensi.data.UserAgendaViewModelFactory
import id.my.matahati.absensi.data.UserAgendaRepository
import androidx.compose.ui.window.Dialog


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
    val context = LocalContext.current

    val absensiViewModel: AbsensiViewModel = viewModel()
    val contractViewModel: UserContractViewModel = viewModel()

    val agendaViewModel: UserAgendaViewModel = viewModel(
        factory = UserAgendaViewModelFactory(
            UserAgendaRepository(context)
        )
    )
    val agendas by agendaViewModel.agendas.collectAsState()
    var showAgendaDialog by remember { mutableStateOf(false) }

    val logs by absensiViewModel.logs.collectAsState()
    val loadingLog by absensiViewModel.loading.collectAsState()
    val errorLog by absensiViewModel.error.collectAsState()

    val contract by contractViewModel.contract.collectAsState()
    val loadingContract by contractViewModel.loading.collectAsState()
    val errorContract by contractViewModel.error.collectAsState()

    val schedules by scheduleViewModel.schedules.collectAsState(initial = emptyList())
    val isLoading by scheduleViewModel.loading.collectAsState()
    val error by scheduleViewModel.error.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }


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

    LaunchedEffect(userId) {
        contractViewModel.loadUserContract(userId)
    }

    LaunchedEffect(currentMonth) {
        agendaViewModel.loadAgenda(currentMonth.toString())
    }

    val calendarBackground = Color(0xFFF5F5F5)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = screenHeight * 0.05f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    bottom = 50.dp
                )
            ) {
                item {
                    // === Judul ===
                    Text(
                        text = "Jadwal & Shift",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                item{
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
                            onDateSelected =
                                {
                                    selectedDate = it
                                    showAgendaDialog = true
                                },
                            today = today,
                            schedules = schedules,
                            agendas = agendas
                        )
                    }

                    // === 🔹 Pesan jika tidak ada shift ===
//                    if (schedules.isEmpty()) {
//                        Text(
//                            text = "📭 Belum ada jadwal shift untuk bulan ini",
//                            color = Color.Gray,
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }

                item {
                    when {
                        loadingContract -> {
                            Text("⏳ Memuat kontrak...")
                        }

                        contract != null -> {
                            contract?.let { kontrak ->
                                KontrakKerjaCard(
                                    kontrak = kontrak
                                )
                            }
                        }

                        else -> {
                            Text(
                                text = "📭 Tidak ada kontrak aktif",
                                color = Color.Gray
                            )
                        }
                    }
                }

                item {
                    // === Card Log Absensi ===
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 30.dp),
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

        if (showAgendaDialog && selectedDate != null) {

            val selected = selectedDate!!

            val agendaToday = agendas.filter { agenda ->
                val start = LocalDate.parse(agenda.startAt.substring(0, 10))
                val end = LocalDate.parse(
                    agenda.endAt?.substring(0, 10)
                        ?: agenda.startAt.substring(0, 10)
                )

                !selected.isBefore(start) && !selected.isAfter(end)
            }

            Dialog(onDismissRequest = { showAgendaDialog = false }) {

                val config = LocalConfiguration.current
                val maxWidth = config.screenWidthDp.dp * 0.95f
                val maxHeight = config.screenHeightDp.dp * 0.80f

                val agendaColors = listOf(
                    Color(0xFFE3F2FD),
                    Color(0xFFFFF3E0),
                    Color(0xFFE8F5E9),
                    Color(0xFFF3E5F5),
                    Color(0xFFFFEBEE)
                )

                Card(
                    modifier = Modifier
                        .widthIn(max = maxWidth)
                        .heightIn(max = maxHeight),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(10.dp)
                ) {

                    Box {

                        /* 🔴 CLOSE BUTTON */
                        IconButton(
                            onClick = { showAgendaDialog = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Red
                            )
                        }

                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth()
                        ) {

                            /* 🔹 HEADER */
                            Text(
                                text = "Agenda ${selected.formatIndonesia()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(12.dp))

                            Divider()

                            Spacer(Modifier.height(12.dp))

                            /* 🔹 LIST */
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                if (agendaToday.isEmpty()) {
                                    item {
                                        Text(
                                            "📭 Tidak ada agenda",
                                            color = Color.Gray
                                        )
                                    }
                                }

                                itemsIndexed(agendaToday) { index, agenda ->

                                    val bgColor =
                                        agendaColors[index % agendaColors.size]

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 5.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = bgColor
                                        ),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp)
                                        ) {

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {

                                                Text(
                                                    text = agenda.title,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                val startTime = agenda.startAt.substring(11, 16)
                                                val endTime = agenda.endAt?.substring(11, 16) ?: startTime

                                                Text(
                                                    text = "$startTime - $endTime",
                                                    fontSize = 12.sp,
                                                    color = Color.DarkGray
                                                )
                                            }

                                            agenda.description?.let {
                                                Spacer(Modifier.height(4.dp))
                                                Text(it, fontSize = 13.sp)
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
    schedules: List<UserSchedule>,
    agendas: List<UserAgenda>
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
                        schedules = schedules,
                        agendas = agendas
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
    schedules: List<UserSchedule>,
    agendas: List<UserAgenda>
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

            val agendaForDate = agendas.filter { agenda ->
                val start = LocalDate.parse(agenda.startAt.substring(0, 10))
                val end = LocalDate.parse(agenda.endAt?.substring(0, 10) ?: agenda.startAt.substring(0, 10))

                !date.isBefore(start) && !date.isAfter(end)
            }

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
                /* 🔴 DOT CONTAINER */
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    scheduleForDate?.let {
                        val colorDot = generateColorFromShift(it.cschedname)

                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(colorDot, CircleShape)
                        )
                    }
                    val agendaCount = agendaForDate.size

                    if (agendaCount == 1) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color.Red, CircleShape)
                        )
                    }

                    if (agendaCount > 1) {
                        Box(
                            modifier = Modifier
                                .background(Color.Red, RoundedCornerShape(6.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = agendaCount.toString(),
                                fontSize = 8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KontrakKerjaCard(
    kontrak: UserContract
) {
    // 🔥 PARSE STRING → LocalDate
    val start = remember(kontrak.startDate) {
        LocalDate.parse(kontrak.startDate)
    }
    val end = remember(kontrak.endDate) {
        LocalDate.parse(kontrak.endDate)
    }

    val today = LocalDate.now()
    val remainingDays =
        java.time.temporal.ChronoUnit.DAYS.between(today, end)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4C4C59))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Kontrak Kerja",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Tanggal kontrak (kiri)
                Text(
                    text = "${start.formatIndonesia()} – ${end.formatIndonesia()}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Sisa hari (kanan)
                Text(
                    text = "(${remainingDays} Hari)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        remainingDays < 0 -> Color.Red
                        remainingDays <= 7 -> Color(0xFFD32F2F)
                        remainingDays <= 30 -> Color(0xFFF57C00)
                        else -> Color(0xFF388E3C)
                    }
                )
            }
        }
    }
}

fun LocalDate.formatIndonesia(): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern(
        "dd MMM yyyy",
        Locale("id", "ID")
    )
    return this.format(formatter)
}