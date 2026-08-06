package id.my.matahati.absensi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Delete
import id.my.matahati.absensi.data.TodoItem
import id.my.matahati.absensi.data.TodoStoreItem
import id.my.matahati.absensi.data.TodoStoreRequest
import id.my.matahati.absensi.data.TodoViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.draw.shadow

fun formatTaskDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "-"
    return try {
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
    val scope = rememberCoroutineScope()

    val myTasks = viewModel.myTasks
    val incomingTasks = viewModel.incomingTasks
    val loading = viewModel.loading
    val errorMessage = viewModel.errorMessage

    val primaryColor = Color(0xFFB63352)
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }


    val subtitle = if (session.isCaptainOrAbove()) {
        "Kelola dan distribusikan task departemen"
    } else {
        "Lihat dan selesaikan task yang ada"
    }
    LaunchedEffect(Unit) {
        viewModel.loadTodo(userId)
        if (session.isCaptainOrAbove()) {
            viewModel.loadDepartments()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSuccess = {
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Task berhasil ditambahkan")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (session.isCaptainOrAbove()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.padding(bottom = 50.dp),
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Task")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Header Background - Immersive
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(193.dp)
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
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Modern Switch Tab
                TodoTabSwitch(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    primaryColor = primaryColor
                )

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
                                bottom = 100.dp
                            )
                        ) {
                            items(currentTasks) { task ->
                                TodoCard(
                                    task = task,
                                    onComplete = {
                                        viewModel.completeTodo(task.nid, userId) {
                                            // Refresh callback handled in VM
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
fun TodoTabSwitch(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    primaryColor: Color
) {
    val containerColor = Color(0xFF8E1D36) // Darker shade of primary
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .padding(4.dp)
    ) {
        val pillWidth = maxWidth / 2
        val offset by animateDpAsState(
            targetValue = if (selectedTab == 0) 0.dp else pillWidth,
            animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f)
        )

        // Sliding Pill
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(pillWidth)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
        )

        // Labels
        Row(modifier = Modifier.fillMaxSize()) {
            TabLabel(
                text = "Task Saya",
                isSelected = selectedTab == 0,
                activeColor = primaryColor,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(0) }
            )
            TabLabel(
                text = "Task Masuk",
                isSelected = selectedTab == 1,
                activeColor = primaryColor,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(1) }
            )
        }
    }
}

@Composable
fun TabLabel(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else Color.White.copy(alpha = 0.9f),
        animationSpec = tween(200)
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun TodoCard(
    task: TodoItem,
    onComplete: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            )
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Status Accent Bar - Fixed: Sharp corners fix
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(if (task.fselesai) Color(0xFF009536) else Color(0xFFEF6C00))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row - Fixed vertical alignment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        StatusBadge(isSelesai = task.fselesai)

//                        Spacer(modifier = Modifier.width(12.dp))
//
//                        Icon(
//                            painter = painterResource(id = R.drawable.auditdept),
//                            contentDescription = null,
//                            tint = Color(0xFFB63352),
//                            modifier = Modifier.size(14.dp)
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text(
//                            text = task.departemen_tujuan,
//                            fontSize = 12.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color(0xFFB63352)
//                        )
                    }

                    // Action Controls - Perfectly Centered Vertically
                    Row(
                        modifier = Modifier.offset(y = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Expand Button
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Checkmark Option
                        if (!task.fselesai) {
                            IconButton(
                                onClick = { showConfirmDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Circle,
                                    contentDescription = "Complete",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selesai",
                                    tint = Color(0xFF009536),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = task.cpermintaan,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                        animationSpec = tween(300)
                    ),
                    exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(
                        animationSpec = tween(200)
                    )
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Metadata Section (Concise & Modern)
                        Surface(
                            color = Color(0xFFF8F9FA),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CompactMetaItem(
                                        icon = Icons.Default.Person,
                                        label = "Pembuat",
                                        value = task.nama_peminta,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (task.fselesai) {
                                        CompactMetaItem(
                                            icon = Icons.Default.Person,
                                            label = "Pelaksana",
                                            value = task.nama_pelaksana ?: "-",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CompactMetaItem(
                                        icon = Icons.Default.AccessTime,
                                        label = "Dibuat",
                                        value = formatTaskDate(task.dminta),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (task.fselesai) {
                                        CompactMetaItem(
                                            icon = Icons.Default.AccessTime,
                                            label = "Selesai",
                                            value = formatTaskDate(task.dselesai),
                                            modifier = Modifier.weight(1f)
                                        )
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

@Composable
fun CompactMetaItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFB63352).copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                maxLines = 1
            )
        }
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray,
            maxLines = 1
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userId = SessionManager(context).getUserId()

    // State for multiple items
    val items = remember {
        mutableStateListOf(
            TodoDraftItem()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Task #${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            if (items.size > 1) {
                                IconButton(
                                    onClick = { items.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Departemen Dropdown
                        ExposedDropdownMenuBox(
                            expanded = item.expanded,
                            onExpandedChange = { item.expanded = !item.expanded }
                        ) {
                            OutlinedTextField(
                                value = item.selectedDept?.cname ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Departemen Tujuan") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(item.expanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFB63352),
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = item.expanded,
                                onDismissRequest = { item.expanded = false }
                            ) {
                                viewModel.departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept.cname) },
                                        onClick = {
                                            item.selectedDept = dept
                                            item.expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Permintaan TextField
                        OutlinedTextField(
                            value = item.requestText,
                            onValueChange = { if (it.length <= 512) item.requestText = it },
                            label = { Text("Permintaan") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFB63352),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                }

                // Tombol Tambah Item
                TextButton(
                    onClick = { items.add(TodoDraftItem()) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambah Item", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            val isValid = items.all { it.selectedDept != null && it.requestText.isNotBlank() }
            
            Button(
                onClick = {
                    if (isValid) {
                        val storeItems = items.map {
                            TodoStoreItem(
                                ndep_tujuan = it.selectedDept!!.nid,
                                cpermintaan = it.requestText
                            )
                        }
                        val request = TodoStoreRequest(
                            user_id = userId,
                            items = storeItems
                        )
                        viewModel.storeTodo(request, onSuccess)
                    }
                },
                enabled = !viewModel.saving && isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009536)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (viewModel.saving) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menyimpan...")
                    }
                } else {
                    Text("Simpan (${items.size})")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

class TodoDraftItem {
    var selectedDept by mutableStateOf<id.my.matahati.absensi.data.DepartmentItem?>(null)
    var requestText by mutableStateOf("")
    var expanded by mutableStateOf(false)
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_taskicon),
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Color.LightGray.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Belum ada task", color = Color.Gray, fontSize = 16.sp)
    }
}
