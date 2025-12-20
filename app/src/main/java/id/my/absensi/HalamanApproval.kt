package id.my.matahati.absensi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import id.my.matahati.absensi.data.ApprovalItem
import id.my.matahati.absensi.data.RetrofitClient
import kotlinx.coroutines.*

class HalamanApproval : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HalamanApprovalScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalamanApprovalScreen() {

    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Absen Manual", "Izin")

    var listData by remember { mutableStateOf<List<ApprovalItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val userId = session.getUserId()

    fun loadData() {
        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val type = if (selectedTab == 0) "mscan_manual" else "mrequest"
                Log.d("APPROVAL", "Load $type userId=$userId")

                val response =
                    RetrofitClient.instance.getApprovalList(type, userId)

                if (response.isSuccessful && response.body()?.success == true) {
                    listData = response.body()?.data ?: emptyList()
                } else {
                    errorMessage = "Gagal memuat data"
                }

            } catch (e: Exception) {
                errorMessage = e.message
                Log.e("APPROVAL", "Error ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab) {
        loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // ===== HEADER =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(Color(0xFFB63352)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "APPROVAL",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        // ===== TAB =====
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFB63352),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 14.sp) }
                )
            }
        }

        // ===== CONTENT =====
        when {
            isLoading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFB63352))
                }
            }

            errorMessage != null -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMessage ?: "Error", color = Color.Red)
                }
            }

            listData.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada data pending", color = Color.Gray)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    items(listData) { item ->
                        ApprovalCard(
                            item = item,
                            onApprove = {
                                handleAction(
                                    userId = userId,
                                    id = item.nid, // ✅ PAKAI nid
                                    type = if (selectedTab == 0) "mscan_manual" else "mrequest",
                                    action = "approve"
                                ) { loadData() }
                            },
                            onReject = {
                                handleAction(
                                    userId = userId,
                                    id = item.nid, // ✅ PAKAI nid
                                    type = if (selectedTab == 0) "mscan_manual" else "mrequest",
                                    action = "reject"
                                ) { loadData() }
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ============================================================
   ACTION APPROVE / REJECT
   ============================================================ */
fun handleAction(
    userId: Int,
    id: Int,
    type: String,
    action: String,
    onDone: () -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.instance.approvalAction(
                userId = userId,
                id = id,
                type = type,
                action = action
            )

            if (response.isSuccessful) {
                Log.d("APPROVAL", "Action $action sukses id=$id")
                withContext(Dispatchers.Main) {
                    onDone()
                }
            } else {
                Log.e("APPROVAL", "Action gagal")
            }

        } catch (e: Exception) {
            Log.e("APPROVAL", "Error action: ${e.message}")
        }
    }
}
