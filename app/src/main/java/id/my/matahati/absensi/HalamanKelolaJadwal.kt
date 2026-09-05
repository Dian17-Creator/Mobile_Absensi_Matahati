package id.my.matahati.absensi

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.matahati.absensi.data.AdminScheduleItem
import id.my.matahati.absensi.data.MasterScheduleItem
import id.my.matahati.absensi.data.RetrofitClientLaravel
import kotlinx.coroutines.launch
import java.time.LocalDate

class HalamanKelolaJadwal : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HalamanKelolaJadwalScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalamanKelolaJadwalScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionManager(context) }
    val currentUserId = session.getUserId()

    var scheduleList by remember { mutableStateOf<List<AdminScheduleItem>>(emptyList()) }
    var masterList by remember { mutableStateOf<List<MasterScheduleItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for editing shift
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedSchedule by remember { mutableStateOf<AdminScheduleItem?>(null) }
    var selectedMasterId by remember { mutableStateOf<Int?>(null) }

    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<AdminScheduleItem?>(null) }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val todayStr = LocalDate.now().toString()
                val resSched = RetrofitClientLaravel.instance.getAdminScheduleList(
                    userId = currentUserId,
                    startDate = todayStr,
                    endDate = todayStr
                )
                val resMaster = RetrofitClientLaravel.instance.getMasterScheduleList()

                if (resSched.isSuccessful && resSched.body()?.success == true) {
                    scheduleList = resSched.body()?.data ?: emptyList()
                } else {
                    errorMessage = "Gagal memuat jadwal (Code: ${resSched.code()})"
                }

                if (resMaster.isSuccessful && resMaster.body()?.success == true) {
                    masterList = resMaster.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Jadwal Shift Karyawan", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFB63352))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFFF5F5))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Error",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (scheduleList.isEmpty()) {
                Text(
                    text = "Tidak ada jadwal untuk hari ini",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(scheduleList) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = item.user?.cname ?: "Karyawan #${item.nuserid}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFB63352)
                                )
                                Text(
                                    text = "Departemen: ${item.user?.department?.cname ?: "-"}",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Tanggal: ${item.dwork}",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Shift: ${item.cschedname ?: "-"} (${item.dstart ?: "-"} - ${item.dend ?: "-"})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedSchedule = item
                                            selectedMasterId = item.nidsched
                                            showEditDialog = true
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB63352))
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            scheduleToDelete = item
                                            showDeleteDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Hapus")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Edit Dialog
            if (showEditDialog && selectedSchedule != null) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Edit Shift Karyawan") },
                    text = {
                        Column {
                            Text("Karyawan: ${selectedSchedule?.user?.cname}")
                            Text("Tanggal: ${selectedSchedule?.dwork}")
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Pilih Master Shift:", fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(modifier = Modifier.height(200.dp)) {
                                items(masterList) { master ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedMasterId == master.nid,
                                            onClick = { selectedMasterId = master.nid }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${master.cname} (${master.dstart ?: "-"} - ${master.dend ?: "-"})")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val schedId = selectedSchedule?.nid
                                val masterId = selectedMasterId
                                if (schedId != null && masterId != null) {
                                    scope.launch {
                                        try {
                                            val res = RetrofitClientLaravel.instance.updateAdminUserSchedule(schedId, masterId)
                                            if (res.isSuccessful && res.body()?.success == true) {
                                                Toast.makeText(context, "Shift berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                                showEditDialog = false
                                                loadData()
                                            } else {
                                                Toast.makeText(context, "Gagal memperbarui shift", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB63352))
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Delete Dialog
            if (showDeleteDialog && scheduleToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Hapus Jadwal Shift") },
                    text = { Text("Apakah Anda yakin ingin menghapus jadwal shift ${scheduleToDelete?.user?.cname} pada tanggal ${scheduleToDelete?.dwork}?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                val schedId = scheduleToDelete?.nid
                                if (schedId != null) {
                                    scope.launch {
                                        try {
                                            val res = RetrofitClientLaravel.instance.deleteAdminUserSchedule(schedId)
                                            if (res.isSuccessful && res.body()?.success == true) {
                                                Toast.makeText(context, "Jadwal berhasil dihapus", Toast.LENGTH_SHORT).show()
                                                showDeleteDialog = false
                                                loadData()
                                            } else {
                                                Toast.makeText(context, "Gagal menghapus jadwal", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Hapus", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}
