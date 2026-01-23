package id.my.matahati.absensi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.matahati.absensi.data.RetrofitClient
import id.my.matahati.absensi.data.RetrofitClientLaravel
import id.my.matahati.absensi.data.SalaryItem
import id.my.matahati.absensi.utils.toRupiah
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign

class HalamanGaji : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HalamanGajiScreen()
        }
    }
}

@Composable
fun HalamanGajiScreen() {

    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val userId = remember { session.getUserId() }

    var gajiList by remember { mutableStateOf<List<SalaryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGaji by remember { mutableStateOf<SalaryItem?>(null) }

    /* ================= LOAD DATA ================= */
    LaunchedEffect(Unit) {

        if (userId == -1) {
            Log.e("GAJI_DEBUG", "User belum login")
            isLoading = false
            return@LaunchedEffect
        }

        try {
            Log.d("GAJI_DEBUG", "USER LOGIN ID = $userId")

            val response = RetrofitClientLaravel.instance.getUserSalary(
                userId = userId
            )

            Log.d("GAJI_DEBUG", "URL = ${response.raw().request.url}")
            Log.d("GAJI_DEBUG", "CODE = ${response.code()}")

            if (response.isSuccessful) {
                gajiList = response.body()?.data ?: emptyList()
                Log.d("GAJI_DEBUG", "DATA SIZE = ${gajiList.size}")
            } else {
                Log.e("GAJI_DEBUG", "ERROR = ${response.errorBody()?.string()}")
            }

        } catch (e: Exception) {
            Log.e("GAJI_DEBUG", "EXCEPTION", e)
        } finally {
            isLoading = false
        }
    }

    /* ================= UI ================= */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            /* ================= TITLE ================= */
            Text(
                text = "Slip Gaji Saya",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            when {

                isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                gajiList.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada data gaji")
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {

                        items(gajiList) { item ->

                            Card(
                                onClick = { selectedGaji = item },
                                modifier = Modifier
                                    .fillMaxWidth(), // 🔥 FULL WIDTH FIX
                                elevation = CardDefaults.cardElevation(6.dp),
                                shape = MaterialTheme.shapes.large
                            ) {

                                Column(
                                    modifier = Modifier.padding(18.dp)
                                ) {

                                    Text(
                                        "Gaji ${item.period_month}/${item.period_year}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text("Jabatan : ${item.jabatan}")
                                    Text("Masuk : ${item.jumlah_masuk} hari")
                                    Text("Total : ${item.total_gaji.toRupiah()}")

                                    Spacer(Modifier.height(8.dp))

                                    val statusColor = when (item.status) {
                                        "APPROVED" -> Color(0xFF2E7D32)
                                        "REJECTED" -> Color.Red
                                        else -> Color(0xFFFF9800)
                                    }

                                    Text(
                                        item.status,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        selectedGaji?.let {
            DetailGajiDialog(
                userId = userId, // 🔥 TAMBAH INI
                gaji = it,
                onDismiss = { selectedGaji = null },
                onUpdated = suspend {
                    isLoading = true

                    val response =
                        RetrofitClientLaravel.instance.getUserSalary(userId)

                    gajiList = response.body()?.data ?: emptyList()

                    isLoading = false
                }
            )
        }
    }
}


/* ===================================================
   DETAIL DIALOG
   =================================================== */
@Composable
fun DetailGajiDialog(
    userId: Int, // 🔥 tambah ini
    gaji: SalaryItem,
    onDismiss: () -> Unit,
    onUpdated: suspend () -> Unit
) {

    val scope = rememberCoroutineScope()

    var showRejectDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun updateStatus(status: String, note: String = "") {

        scope.launch { // 🔥 coroutine aman
            try {
                isLoading = true

                RetrofitClientLaravel.instance.updateSalaryStatus(
                    userId = userId,
                    year = gaji.period_year,
                    month = gaji.period_month,
                    status = status,
                    note = note
                )


                onUpdated()
                onDismiss()

            } catch (e: Exception) {
                Log.e("GAJI_UPDATE", "error", e)
            } finally {
                isLoading = false
            }
        }
    }

    /* ================= DIALOG ================= */
    Dialog(onDismissRequest = onDismiss) {

        Card(shape = MaterialTheme.shapes.large) {

            Box {

                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
//                    IconButton(
//                        onClick = onDismiss,
//                        modifier = Modifier.align(Alignment.End)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Close,
//                            contentDescription = "Close",
//                            tint = Color.Red,
//                            modifier = Modifier.size(24.dp)
//                        )
//                    }

                    /* ================= HEADER ================= */
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Slip Gaji ${gaji.period_month}/${gaji.period_year}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    /* ================= INFO ================= */
                    Text("Jabatan : ${gaji.jabatan}")
                    Text("Masuk   : ${gaji.jumlah_masuk} hari")

                    Divider(Modifier.padding(vertical = 8.dp))

                    /* ================= PENGHASILAN ================= */
                    Text("PENGHASILAN", fontWeight = FontWeight.Bold)

                    RowItem("Gaji Pokok", gaji.gaji_pokok.toRupiah())
                    RowItem("Tunjangan Makan", gaji.tunjangan_makan.toRupiah())
                    RowItem("Tunjangan Jabatan", gaji.tunjangan_jabatan.toRupiah())
                    RowItem("Tunjangan Transport", gaji.tunjangan_transport.toRupiah())
                    RowItem("Tunjangan Luar Kota", gaji.tunjangan_luar_kota.toRupiah())
                    RowItem("Tunjangan Masa Kerja", gaji.tunjangan_masa_kerja.toRupiah())
                    RowItem("Gaji Lembur", gaji.gaji_lembur.toRupiah())
                    RowItem("Tabungan Diambil", gaji.tabungan_diambil.toRupiah())

                    Divider(Modifier.padding(vertical = 8.dp))

                    /* ================= POTONGAN ================= */
                    Text("POTONGAN", fontWeight = FontWeight.Bold)

                    RowItem("Keterlambatan", gaji.potongan_keterlambatan.toRupiah())
                    RowItem("Lain-Lain", gaji.potongan_lain.toRupiah())
                    RowItem("Tabungan", gaji.potongan_tabungan.toRupiah())

                    Divider(Modifier.padding(vertical = 8.dp))

                    RowItem(
                        "GAJI DITERIMA",
                        gaji.total_gaji.toRupiah(),
                        true
                    )

                    gaji.note?.let {
                        Divider(Modifier.padding(vertical = 8.dp))
                        Text("Catatan HR:")
                        Text(it)
                    }

                    Spacer(Modifier.height(20.dp))

                    /* ================= BUTTON ================= */
                    if (gaji.status == "PENDING") {

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { showRejectDialog = true }
                            ) {
                                Text("Reject")
                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { updateStatus("APPROVED") }
                            ) {
                                Text("Approve")
                            }
                        }
                    }
                }


                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    if (showRejectDialog) {
        RejectReasonDialog(
            onDismiss = { showRejectDialog = false },
            onSubmit = {
                showRejectDialog = false
                updateStatus("REJECTED", it)
            }
        )
    }
}

@Composable
fun RejectReasonDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alasan Penolakan") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Isi alasan...") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = reason.isNotBlank(),
                onClick = { onSubmit(reason) }
            ) {
                Text("Kirim")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}


@Composable
fun RowItem(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}
