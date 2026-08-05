package id.my.matahati.absensi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.matahati.absensi.data.TodoItem
import id.my.matahati.absensi.data.TodoViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

fun formatTaskDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "-"
    return try {
        // Handle ISO format from Laravel: "2026-08-05T07:18:53.000000Z" or "2026-08-05 08:15:00"
        val inputFormat = if (dateString.contains("T")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        }
        
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault())
        outputFormat.format(date!!)
    } catch (_: Exception) {
        dateString
    }
}

@Composable
fun TodoScreen(
    viewModel: TodoViewModel = viewModel()
) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val userId = session.getUserId()

    val myTasks = viewModel.myTasks
    val incomingTasks = viewModel.incomingTasks
    val loading = viewModel.loading
    val errorMessage = viewModel.errorMessage

    val primaryColor = Color(0xFFB63352)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Task Saya", "Task Masuk")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadTodo(userId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header Background - Immersive (Edge-to-Edge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(BottomCurveShape(curveHeight = 50f))
                    .background(primaryColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "To Do List",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Kelola seluruh task departemen",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = primaryColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = primaryColor
                        )
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content
                if (loading && myTasks.isEmpty() && incomingTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else {
                    val currentTasks = if (selectedTab == 0) myTasks else incomingTasks
                    
                    if (currentTasks.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = paddingValues.calculateBottomPadding() + 24.dp
                            )
                        ) {
                            items(currentTasks) { task ->
                                TodoCard(
                                    task = task,
                                    isMyTask = selectedTab == 0,
                                    onComplete = {
                                        viewModel.completeTodo(task.nid, userId) {
                                            // Optional success callback
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodoCard(
    task: TodoItem,
    isMyTask: Boolean,
    onComplete: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Konfirmasi") },
            text = { Text("Apakah Anda yakin ingin menyelesaikan task ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009536))
                ) {
                    Text("Ya")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Status Accent Bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(if (task.fselesai) Color(0xFF009536) else Color(0xFFEF6C00))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(isSelesai = task.fselesai)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.departemen_tujuan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = task.cpermintaan,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(14.dp))

                if (!isMyTask) {
                    ModernInfoRow(icon = Icons.Default.Person, label = "Pembuat", value = task.nama_peminta)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                ModernInfoRow(icon = Icons.Default.AccessTime, label = "Dibuat", value = formatTaskDate(task.dminta))
                
                if (task.fselesai) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ModernInfoRow(icon = Icons.Default.AccessTime, label = "Selesai", value = formatTaskDate(task.dselesai))
                    Spacer(modifier = Modifier.height(6.dp))
                    ModernInfoRow(icon = Icons.Default.Person, label = "Pelaksana", value = task.nama_pelaksana ?: "-")
                }

                if (!task.fselesai) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Detail Action */ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text("Detail", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009536)),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                if (isMyTask) "Selesai" else "Selesaikan", 
                                color = Color.White, 
                                fontSize = 14.sp,
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
fun ModernInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(60.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusBadge(isSelesai: Boolean) {
    val bgColor = if (isSelesai) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val textColor = if (isSelesai) Color(0xFF2E7D32) else Color(0xFFEF6C00)
    val label = if (isSelesai) "Selesai" else "Pending"

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Checklist,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Belum ada task.", color = Color.Gray, fontSize = 16.sp)
    }
}
