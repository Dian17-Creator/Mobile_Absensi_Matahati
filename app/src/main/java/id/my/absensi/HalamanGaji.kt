package id.my.matahati.absensi

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

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

    val primaryColor = Color(0xFFB63352)

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
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(BottomCurveShape(curveHeight = 50f))
                .background(primaryColor)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(25.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { (context as Activity).finish() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFFFFFFF))
                }

                /* ================= TITLE ================= */
                Text(
                    text = "Slip Gaji Saya",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

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
                        modifier = Modifier.weight(1f), // 🔥 INI KUNCINYA
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {

                        items(gajiList) { item ->

                            SalaryCard(
                                item = item,
                                onClick = { selectedGaji = item }
                            )
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


//Detail Dialog
@Composable
fun DetailGajiDialog(
    userId: Int, // 🔥 tambah ini
    gaji: SalaryItem,
    onDismiss: () -> Unit,
    onUpdated: suspend () -> Unit
) {
    val config = LocalConfiguration.current

    val maxWidth = config.screenWidthDp.dp * 0.95f
    val maxHeight = config.screenHeightDp.dp * 0.85f

    val isSmall = config.screenWidthDp < 360
    val textSize = if (isSmall) 12.sp else 14.sp

    val borderColor = when (gaji.status) {
        "APPROVED" -> Color(0xFF2E7D32) // hijau
        "REJECTED" -> Color(0xFFD32F2F) // merah
        else -> Color(0xFFFF9800) // orange (pending)
    }

    val scope = rememberCoroutineScope()
    val primaryColor = Color(0xFFB63352)

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

        Card(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .heightIn(max = maxHeight),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, borderColor), // 🔥 INI
            elevation = CardDefaults.cardElevation(10.dp)
        ) {

            Box {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align (Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {


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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            /* 🔴 Reject (outline primary) */
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { showRejectDialog = true },
                                shape = RoundedCornerShape(50), // 🔥 pill
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(primaryColor)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = primaryColor
                                )
                            ) {
                                Text("Reject")
                            }

                            /* 🟣 Approve (solid primary) */
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { updateStatus("APPROVED") },
                                shape = RoundedCornerShape(50), // 🔥 pill
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.White
                                )
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
    val primaryColor = Color(0xFFB63352)

    AlertDialog(
        onDismissRequest = onDismiss,

        shape = RoundedCornerShape(22.dp), // 🔥 rounded dialog

        title = {
            Text(
                "Alasan Penolakan",
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        },

        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Isi alasan...") },
                modifier = Modifier.fillMaxWidth(),

                /* 🔥 warna focus ikut primary */
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor
                ),

                shape = RoundedCornerShape(14.dp)
            )
        },

        /* 🔥 tombol kirim solid primary */
        confirmButton = {
            Button(
                enabled = reason.isNotBlank(),
                onClick = { onSubmit(reason) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                )
            ) {
                Text("Kirim", fontWeight = FontWeight.SemiBold)
            }
        },

        /* 🔥 batal outline primary */
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = primaryColor
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(primaryColor)
                )
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun SalaryCard(
    item: SalaryItem,
    onClick: () -> Unit
) {
    val statusColor = when (item.status) {
        "APPROVED" -> Color(0xFF2E7D32)
        "REJECTED" -> Color(0xFFD32F2F)
        else -> Color(0xFFFF9800)
    }

    val borderColor = when (item.status) {
        "APPROVED" -> Color(0xFF2E7D32)
        "REJECTED" -> Color(0xFFD32F2F)
        else -> Color(0xFFFF9800)
    }

    val primaryColor = Color(0xFFB63352)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(10.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            /* ===== Header Row ===== */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "${item.period_month}/${item.period_year}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                /* 🔥 status badge */
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(15))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        item.status,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            /* 🔥 TOTAL GAJI BESAR */
            Text(
                text = item.total_gaji.toRupiah(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            Spacer(Modifier.height(10.dp))

            Divider(color = Color(0xFFEAEAEA))

            Spacer(Modifier.height(10.dp))

            /* ===== Info kecil ===== */
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Jabatan", fontSize = 12.sp, color = Color.Gray)
                    Text(item.jabatan, fontWeight = FontWeight.Medium)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Masuk", fontSize = 12.sp, color = Color.Gray)
                    Text("${item.jumlah_masuk} hari", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun RowItem(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        /* ===== LABEL ===== */
        Text(
            text = label,
            modifier = Modifier.weight(2f),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )

        /* ===== RP (CENTERED COLUMN) ===== */
        Text(
            text = "Rp",
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.6f),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )

        /* ===== NOMINAL (RIGHT ALIGN biar rapi angka) ===== */
        Text(
            text = value,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Bold
        )
    }
}
