package id.my.matahati.absensi

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
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
import id.my.matahati.absensi.data.DepartmentItem
import id.my.matahati.absensi.data.MasterScheduleItem
import id.my.matahati.absensi.data.RetrofitClientLaravel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

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
    var departmentList by remember { mutableStateOf<List<DepartmentItem>>(emptyList()) }
    
    var selectedDeptId by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var expandedDeptDropdown by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for editing shift
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedSchedule by remember { mutableStateOf<AdminScheduleItem?>(null) }
    var selectedMasterId by remember { mutableStateOf<Int?>(null) }

    // Delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<AdminScheduleItem?>(null) }

    fun loadDepartments() {
        scope.launch {
            try {
                val res = RetrofitClientLaravel.instance.getDepartments()
                if (res.isSuccessful && res.body()?.success == true) {
                    departmentList = res.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val resSched = RetrofitClientLaravel.instance.getAdminScheduleList(
                    userId = currentUserId,
                    departmentId = selectedDeptId,
                    startDate = selectedDate,
                    endDate = selectedDate
                )
                val resMaster = RetrofitClientLaravel.instance.getMasterScheduleList()

                if (resSched.isSuccessful && resSched.body()?.success == true) {
                    scheduleList = resSched.body()?.data ?: emptyList()
                } else {
                    val err = resSched.errorBody()?.string() ?: "no error body"
                    Log.e("SCHEDULE_DEBUG", "Load sched failed: code=${resSched.code()} err=$err")
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
        loadDepartments()
        loadData()
    }

    LaunchedEffect(selectedDeptId, selectedDate) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Jadwal Shift Karyawan", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFB63352))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFFF5F5))
        ) {
            // Filter Bar (Date & Department)
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFB63352), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tanggal:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                runCatching {
                                    val parsed = LocalDate.parse(selectedDate)
                                    calendar.set(parsed.year, parsed.monthValue - 1, parsed.dayOfMonth)
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB63352)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(selectedDate, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Department Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFFB63352), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Departemen:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier.width(200.dp)
                        ) {
                            val selectedName = departmentList.find { it.nid == selectedDeptId }?.cname ?: "Semua Departemen"
                            ExposedDropdownMenuBox(
                                expanded = expandedDeptDropdown,
                                onExpandedChange = { expandedDeptDropdown = !expandedDeptDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedName,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFB63352),
                                        focusedLabelColor = Color(0xFFB63352),
                                        unfocusedBorderColor = Color.LightGray
                                    ),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDeptDropdown)
                                    },
                                    singleLine = true
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedDeptDropdown,
                                    onDismissRequest = { expandedDeptDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Semua Departemen") },
                                        onClick = {
                                            selectedDeptId = null
                                            expandedDeptDropdown = false
                                        }
                                    )
                                    departmentList.forEach { dept ->
                                        DropdownMenuItem(
                                            text = { Text(dept.cname) },
                                            onClick = {
                                                selectedDeptId = dept.nid
                                                expandedDeptDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
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
                        text = "Tidak ada jadwal untuk tanggal $selectedDate",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(scheduleList) { item ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.user?.cname ?: "Karyawan #${item.nuserid}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color(0xFFB63352)
                                        )

                                        // Department Badge
                                        Surface(
                                            color = Color(0xFFFFF0F3),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.user?.department?.cname ?: "-",
                                                color = Color(0xFFB63352),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Nama Shift : ${item.cschedname ?: "-"}",
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "Jam : ${item.dstart ?: "-"} - ${item.dend ?: "-"}" +
                                                if (!item.dstart2.isNullOrEmpty()) " | ${item.dstart2} - ${item.dend2}" else "",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedSchedule = item
                                                selectedMasterId = item.nidsched
                                                showEditDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB63352)),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Edit", fontSize = 13.sp)
                                        }

                                        Button(
                                            onClick = {
                                                scheduleToDelete = item
                                                showDeleteDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Hapus", fontSize = 13.sp, color = Color.White)
                                        }
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
                    title = { Text("Edit Shift Karyawan", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Karyawan: ${selectedSchedule?.user?.cname}", fontWeight = FontWeight.Medium)
                            Text("Tanggal: ${selectedSchedule?.dwork}", fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Pilih Master Shift:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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
                                        Text("${master.cname} (${master.dstart ?: "-"} - ${master.dend ?: "-"})", fontSize = 13.sp)
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
                                                val err = res.errorBody()?.string() ?: "no error body"
                                                Log.e("SCHEDULE_DEBUG", "Update failed: code=${res.code()} err=$err")
                                                Toast.makeText(context, "Gagal (${res.code()}): $err", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("SCHEDULE_DEBUG", "Update exception: ${e.message}", e)
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB63352)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Batal", color = Color.Gray)
                        }
                    }
                )
            }

            // Delete Dialog
            if (showDeleteDialog && scheduleToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Hapus Jadwal Shift", fontWeight = FontWeight.Bold) },
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
                                                val err = res.errorBody()?.string() ?: "no error body"
                                                Log.e("SCHEDULE_DEBUG", "Delete failed: code=${res.code()} err=$err")
                                                Toast.makeText(context, "Gagal (${res.code()}): $err", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("SCHEDULE_DEBUG", "Delete exception: ${e.message}", e)
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Hapus", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Batal", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}
