package id.my.matahati.absensi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                gaji = it,
                onDismiss = { selectedGaji = null }
            )
        }
    }
}


/* ===================================================
   DETAIL DIALOG
   =================================================== */

@Composable
fun DetailGajiDialog(
    gaji: SalaryItem,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        title = {
            Text("Slip Gaji ${gaji.period_month}/${gaji.period_year}",
                fontWeight = FontWeight.Bold)
        },
        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                /* ================= INFO ================= */
                Text("Jabatan : ${gaji.jabatan}")
                Text("Masuk   : ${gaji.jumlah_masuk} hari")

                Divider()

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

                Divider()
                Text("POTONGAN", fontWeight = FontWeight.Bold)

                RowItem("Keterlambatan", gaji.potongan_keterlambatan.toRupiah())
                RowItem("Lain-Lain", gaji.potongan_lain.toRupiah())
                RowItem("Tabungan", gaji.potongan_tabungan.toRupiah())

                Divider(thickness = 2.dp)

                RowItem(
                    "GAJI DITERIMA",
                    gaji.total_gaji.toRupiah(),
                    isBold = true
                )

                gaji.note?.let {
                    Divider()
                    Text("Catatan HR:")
                    Text(it)
                }
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
