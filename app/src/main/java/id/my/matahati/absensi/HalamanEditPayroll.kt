package id.my.matahati.absensi

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.matahati.absensi.data.PayrollUpdateRequest
import id.my.matahati.absensi.data.PayrollViewModel

class HalamanEditPayroll : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            MaterialTheme {

                HalamanEditPayrollScreen()
            }
        }
    }
}

@Composable
fun HalamanEditPayrollScreen(
    viewModel: PayrollViewModel = viewModel()
) {

    val context = LocalContext.current

    val activity = context as HalamanEditPayroll

    val payrollId =
        activity.intent.getIntExtra(
            "payroll_id",
            0
        )

    val primaryColor = Color(0xFFB63352)

    /* =======================================================
     * LOAD DETAIL
     * ======================================================= */

    LaunchedEffect(Unit) {

        viewModel.loadPayrollDetail(
            payrollId
        )
    }

    val payroll = viewModel.selectedPayroll

    /* =======================================================
     * FORM STATE
     * ======================================================= */

    var jumlahMasuk by remember {
        mutableStateOf("")
    }

    var gajiPokok by remember {
        mutableStateOf("")
    }

    var tunjanganMakan by remember {
        mutableStateOf("")
    }

    var tunjanganJabatan by remember {
        mutableStateOf("")
    }

    var tunjanganTransport by remember {
        mutableStateOf("")
    }

    var bonusKehadiran by remember {
        mutableStateOf("")
    }

    var tunjanganLuarKota by remember {
        mutableStateOf("")
    }

    var tunjanganMasaKerja by remember {
        mutableStateOf("")
    }

    var tunjanganBackup by remember {
        mutableStateOf("")
    }

    var gajiLembur by remember {
        mutableStateOf("")
    }

    var tabunganDiambil by remember {
        mutableStateOf("")
    }

    var potonganLain by remember {
        mutableStateOf("")
    }

    var potonganTabungan by remember {
        mutableStateOf("")
    }

    var potonganKeterlambatan by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    var reasonEdit by remember {
        mutableStateOf("")
    }

    /* =======================================================
     * SET INITIAL VALUE
     * ======================================================= */

    LaunchedEffect(payroll) {

        payroll?.let {

            jumlahMasuk =
                (it.jumlah_masuk ?: 0).toString()

            gajiPokok =
                it.gaji_pokok?.toString() ?: ""

            tunjanganMakan =
                it.tunjangan_makan?.toString() ?: ""

            tunjanganJabatan =
                it.tunjangan_jabatan?.toString() ?: ""

            tunjanganTransport =
                it.tunjangan_transport?.toString() ?: ""

            bonusKehadiran =
                it.bonus_kehadiran?.toString() ?: ""

            tunjanganLuarKota =
                it.tunjangan_luar_kota?.toString() ?: ""

            tunjanganMasaKerja =
                it.tunjangan_masa_kerja?.toString() ?: ""

            tunjanganBackup =
                it.tunjangan_backup?.toString() ?: ""

            gajiLembur =
                it.gaji_lembur?.toString() ?: ""

            tabunganDiambil =
                it.tabungan_diambil?.toString() ?: ""

            potonganLain =
                it.potongan_lain?.toString() ?: ""

            potonganTabungan =
                it.potongan_tabungan?.toString() ?: ""

            potonganKeterlambatan =
                it.potongan_keterlambatan?.toString() ?: ""

            note =
                it.note ?: ""

            reasonEdit =
                it.reasonedit ?: ""
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        /* ================= HEADER ================= */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(
                    BottomCurveShape(
                        curveHeight = 50f
                    )
                )
                .background(primaryColor)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp)
        ) {

            /* ================= TOP BAR ================= */

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                IconButton(
                    onClick = {
                        (context as Activity).finish()
                    },
                    modifier = Modifier.align(
                        Alignment.CenterStart
                    )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ArrowBack,

                        contentDescription = null,

                        tint = Color.White
                    )
                }

                Text(
                    text = "Edit Payroll",

                    modifier = Modifier.fillMaxWidth(),

                    textAlign = TextAlign.Center,

                    fontSize = 24.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            /* ================= CONTENT ================= */

            if (payroll == null) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = primaryColor
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(18.dp),

                    contentPadding =
                        PaddingValues(bottom = 24.dp)
                ) {

                    /* ================= USER INFO ================= */

                    item {

                        Card(
                            shape = RoundedCornerShape(16.dp),

                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = 8.dp
                                ),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor = Color.White)

                        ) {

                            Row(

                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 18.dp,
                                        vertical = 16.dp
                                    ),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Text(

                                    text =
                                        payroll.user_name ?: "-",

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 20.sp
                                )

                                Surface(

                                    shape = RoundedCornerShape(12.dp),

                                    color =
                                        primaryColor.copy(alpha = 0.12f)
                                ) {

                                    Text(

                                        text =
                                            payroll.jabatan ?: "-",

                                        color = primaryColor,

                                        fontWeight = FontWeight.SemiBold,

                                        fontSize = 13.sp,

                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 6.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    /* ================= DATA GAJI ================= */

                    item {

                        Card(

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(16.dp),

                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = 8.dp
                                ),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                        ) {

                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {

                                /* ================= DATA GAJI ================= */

                                Text(
                                    text = "Data Gaji",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                PayrollTextField(
                                    label = "Jumlah Masuk",
                                    value = jumlahMasuk,
                                    onValueChange = {
                                        jumlahMasuk = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Gaji Pokok",
                                    value = gajiPokok,
                                    onValueChange = {
                                        gajiPokok = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Tunjangan Makan",
                                    value = tunjanganMakan,
                                    onValueChange = {
                                        tunjanganMakan = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Tunjangan Jabatan",
                                    value = tunjanganJabatan,
                                    onValueChange = {
                                        tunjanganJabatan = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Tunjangan Transport",
                                    value = tunjanganTransport,
                                    onValueChange = {
                                        tunjanganTransport = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Tunjangan Luar Kota",
                                    value = tunjanganLuarKota,
                                    onValueChange = {
                                        tunjanganLuarKota = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Tunjangan Masa Kerja",
                                    value = tunjanganMasaKerja,
                                    onValueChange = {
                                        tunjanganMasaKerja = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Tunjangan Backup",
                                    value = tunjanganBackup,
                                    onValueChange = {
                                        tunjanganBackup = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Gaji Lembur",
                                    value = gajiLembur,
                                    onValueChange = {
                                        gajiLembur = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Bonus Kehadiran",
                                    value = bonusKehadiran,
                                    onValueChange = {
                                        bonusKehadiran = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Tabungan Diambil",
                                    value = tabunganDiambil,
                                    onValueChange = {
                                        tabunganDiambil = it
                                    }
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                /* ================= POTONGAN ================= */

                                SectionTitle(
                                    title = "Potongan"
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                PayrollTextField(
                                    label = "Potongan Lain",
                                    value = potonganLain,
                                    onValueChange = {
                                        potonganLain = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Potongan Tabungan",
                                    value = potonganTabungan,
                                    onValueChange = {
                                        potonganTabungan = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Potongan Keterlambatan",
                                    value = potonganKeterlambatan,
                                    onValueChange = {
                                        potonganKeterlambatan = it
                                    }
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                /* ================= KETERANGAN ================= */

                                SectionTitle(
                                    title = "Keterangan"
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                PayrollTextField(
                                    label = "Alasan Edit",
                                    value = reasonEdit,
                                    onValueChange = {
                                        reasonEdit = it
                                    }
                                )

                                PayrollTextField(
                                    label = "Note",
                                    value = note,
                                    onValueChange = {
                                        note = it
                                    }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                /* ================= SAVE BUTTON ================= */

                                Button(

                                    onClick = {

                                        val request =
                                            PayrollUpdateRequest(

                                                jumlah_masuk =
                                                    jumlahMasuk.toIntOrNull() ?: 0,

                                                gaji_pokok =
                                                    gajiPokok,

                                                gaji_harian = "",

                                                tunjangan_makan =
                                                    tunjanganMakan,

                                                tunjangan_jabatan =
                                                    tunjanganJabatan,

                                                tunjangan_transport =
                                                    tunjanganTransport,

                                                tunjangan_backup = "",

                                                tunjangan_luar_kota = "",

                                                tunjangan_masa_kerja = "",

                                                gaji_lembur = "",

                                                bonus_kehadiran =
                                                    bonusKehadiran,

                                                potongan_lain =
                                                    potonganLain,

                                                potongan_tabungan =
                                                    potonganTabungan,

                                                potongan_keterlambatan =
                                                    potonganKeterlambatan,

                                                tabungan_diambil = "",

                                                note = note,

                                                reasonedit =
                                                    reasonEdit
                                            )

                                        viewModel.updatePayroll(

                                            id = payrollId,

                                            request = request,

                                            onSuccess = {

                                                Toast.makeText(

                                                    context,

                                                    "Payroll berhasil diupdate",

                                                    Toast.LENGTH_SHORT

                                                ).show()

                                                activity.finish()
                                            }
                                        )
                                    },

                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),

                                    shape = RoundedCornerShape(18.dp),

                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                primaryColor
                                        )
                                ) {

                                    Text(
                                        text = "Simpan Perubahan",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
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

@Composable
fun PayrollTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(

        value = value,

        onValueChange = onValueChange,

        modifier = Modifier.fillMaxWidth(),

        singleLine = true,

        shape = RoundedCornerShape(16.dp),

        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType.Number
            ),

        label = {
            Text(label)
        }
    )

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun SectionTitle(
    title: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically
    ) {

        HorizontalDivider(
            modifier = Modifier.weight(1f)
        )

        Text(

            text = title,

            modifier = Modifier.padding(
                horizontal = 12.dp
            ),

            fontWeight = FontWeight.Bold,

            fontSize = 17.sp,

            color = Color.Black
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(18.dp))
}