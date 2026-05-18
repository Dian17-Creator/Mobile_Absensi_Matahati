@file:OptIn(ExperimentalMaterial3Api::class)

package id.my.matahati.absensi

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.matahati.absensi.data.DepartmentItem
import id.my.matahati.absensi.data.RekeningItem
import id.my.matahati.absensi.data.RetrofitClient
import id.my.matahati.absensi.data.RetrofitClientLaravel
import id.my.matahati.absensi.data.UserStoreRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.ui.unit.coerceAtLeast

class HalamanTambahUser : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    HalamanTambahUserUI()
                }
            }
        }
    }
}

@Composable
fun HalamanTambahUserUI() {

    val primaryColor = Color(0xFFB63352)
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var gmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var telepon by remember { mutableStateOf("") }
    var ktp by remember { mutableStateOf("") }
    var fingerId by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var namaLengkap by remember { mutableStateOf("") }
    var rekening by remember { mutableStateOf("") }

    var selectedBank by remember {
        mutableStateOf("")
    }

    var selectedDepartment by remember {
        mutableStateOf<DepartmentItem?>(null)
    }

    var selectedPayrollDepartment by remember {
        mutableStateOf<DepartmentItem?>(null)
    }

    val bankList = listOf(
        "Mandiri",
        "BCA",
        "BRI"
    )

    var selectedRekening by remember {
        mutableStateOf<RekeningItem?>(null)
    }

    var departments by remember {
        mutableStateOf<List<DepartmentItem>>(emptyList())
    }

    var rekeningList by remember {
        mutableStateOf<List<RekeningItem>>(emptyList())
    }

    LaunchedEffect(Unit) {

        try {

            val response =
                RetrofitClientLaravel.instance.getDepartments()

            if (response.isSuccessful) {

                departments =
                    response.body()?.data ?: emptyList()

            }

            val rekeningResponse =
                RetrofitClientLaravel.instance.getMandiriRekening()

            if (rekeningResponse.isSuccessful) {

                rekeningList =
                    rekeningResponse.body()?.data ?: emptyList()
            }

        } catch (e: Exception) {

            e.printStackTrace()

        }
    }

    var tanggalMasuk by remember { mutableStateOf("") }

    var selectedRole by remember {
        mutableStateOf("Crew")
    }

    val datePickerState = rememberDatePickerState()

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    val scaleFactor = rememberAdaptiveScale()

    if (showDatePicker) {

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        val millis =
                            datePickerState.selectedDateMillis

                        if (millis != null) {

                            val sdf = java.text.SimpleDateFormat(
                                "yyyy-MM-dd",
                                java.util.Locale("id", "ID")
                            )

                            tanggalMasuk =
                                sdf.format(java.util.Date(millis))
                        }

                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Batal")
                }
            }
        ) {

            DatePicker(
                state = datePickerState
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // BACKGROUND BAWAH
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((445.dp * scaleFactor).coerceAtLeast(250.dp))
                .align(Alignment.BottomCenter)
                .semiCircleTop()
                .background(primaryColor)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Tambah User",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // GAMBAR
            Image(
                painter = painterResource(id = R.drawable.user),
                contentDescription = null,
                modifier = Modifier
                    .size(225.dp).padding(top = 4.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            val scrollState = rememberScrollState()

            // CARD FORM
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),

                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .padding(end = 6.dp) // kasih ruang untuk scrollbar
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = "Data User",
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField(
                            label = "Username or Email",
                            value = username,
                        ) {
                            username = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField("Gmail", gmail) {
                            gmail = it
                        }


                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField("Password", password) {
                            password = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField("No. Telepon", telepon) {
                            telepon = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField("No. KTP", ktp) {
                            ktp = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField("Finger ID", fingerId) {
                            fingerId = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField("Nama", nama) {
                            nama = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField("Nama Lengkap", namaLengkap) {
                            namaLengkap = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = tanggalMasuk,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            label = {
                                Text("Tanggal Masuk")
                            },
                            trailingIcon = {

                                IconButton(
                                    onClick = {
                                        showDatePicker = true
                                    }
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null
                                    )
                                }
                            },
                            shape = RoundedCornerShape(5.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomField("Nomor Rekening", rekening) {
                            rekening = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomDropdown(
                            label = "Jenis Bank",
                            items = bankList,
                            selectedItem = selectedBank,
                            onItemSelected = {
                                selectedBank = it
                            }
                        )

                        if (selectedBank == "Mandiri") {

                            Spacer(modifier = Modifier.height(12.dp))

                            CustomDropdown(
                                label = "Pilih Rekening Sumber",

                                items = rekeningList.map {
                                    "${it.bank} - ${it.nomor_rekening} (${it.atasNama ?: "-"})"
                                },

                                selectedItem =
                                    if (selectedRekening != null)
                                        "${selectedRekening!!.bank} - ${selectedRekening!!.nomor_rekening} (${selectedRekening!!.atasNama ?: "-"})"
                                    else "",

                                onItemSelected = { selected ->

                                    selectedRekening =
                                        rekeningList.find {
                                            "${it.bank} - ${it.nomor_rekening} (${it.atasNama ?: "-"})" == selected
                                        }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomDropdown(
                            label = "Departemen",
                            items = departments.map {
                                it.cname
                            },
                            selectedItem =
                                selectedDepartment?.cname ?: "",
                            onItemSelected = { selected ->
                                selectedDepartment =
                                    departments.find {
                                        it.cname == selected
                                    }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CustomDropdown(
                            label = "Payroll Department",
                            items = departments.map {
                                it.cname
                            },
                            selectedItem =
                                selectedPayrollDepartment?.cname ?: "",
                            onItemSelected = { selected ->
                                selectedPayrollDepartment =
                                    departments.find {
                                        it.cname == selected
                                    }
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Role User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val roleList = listOf(
                            "Captain",
                            "Supervisor",
                            "Senior",
                            "Crew"
                        )

                        Column {

                            roleList.forEach { role ->

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    RadioButton(
                                        selected = selectedRole == role,
                                        onClick = {
                                            selectedRole = role
                                        }
                                    )

                                    Text(
                                        text = role,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {

                                val roleValue = when(selectedRole) {
                                    "Captain" -> "fadmin"
                                    "Supervisor" -> "fsuper"
                                    "Senior" -> "fsenior"
                                    else -> "crew"
                                }

                                val request = UserStoreRequest(

                                    email = username,
                                    name = nama,
                                    cfullname = namaLengkap.ifBlank { null },
                                    password = password,
                                    cmailaddress = gmail.ifBlank { null },
                                    niddept = selectedDepartment?.nid ?: 0,
                                    niddeptpayroll = selectedPayrollDepartment?.nid,
                                    cphone = telepon.ifBlank { null },
                                    cktp = ktp.ifBlank { null },
                                    caccnumber = rekening.ifBlank { null },

                                    finger_id =
                                        if (fingerId.isBlank())
                                            null
                                        else
                                            fingerId.toInt(),

                                    dtanggalmasuk = tanggalMasuk.ifBlank { null },
                                    rekening_id = selectedRekening?.id,
                                    bank = selectedBank.ifBlank { null },
                                    role = roleValue
                                )

                                Log.d("STORE_USER", "REQUEST = $request")

                                CoroutineScope(Dispatchers.IO).launch {

                                    try {

                                        val response =
                                            RetrofitClientLaravel
                                                .instance
                                                .storeUser(request)

                                        Log.d("STORE_USER", "CODE = ${response.code()}")
                                        Log.d("STORE_USER", "BODY = ${response.body()}")
                                        Log.d("STORE_USER", "ERROR = ${response.errorBody()?.string()}")

                                        withContext(Dispatchers.Main) {

                                            if (
                                                response.isSuccessful &&
                                                response.body()?.success == true
                                            ) {

                                                Toast.makeText(
                                                    context,
                                                    "User berhasil ditambahkan",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                            } else {

                                                Toast.makeText(
                                                    context,
                                                    response.body()?.message
                                                        ?: "Gagal tambah user",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }

                                    } catch (e: Exception) {

                                        withContext(Dispatchers.Main) {

                                            Toast.makeText(
                                                context,
                                                e.message ?: "Terjadi kesalahan",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4C4C59)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {

                            Text(
                                text = "Simpan",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .width(4.dp)
                            .height(350.dp) // tinggi scrollbar area
                            .background(
                                Color.LightGray.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(
                                    y = (
                                            (scrollState.value.toFloat() /
                                                    (scrollState.maxValue + 1)) * 225
                                            ).dp
                                )
                                .width(4.dp)
                                .height(125.dp)
                                .background(
                                    Color(0xFFB63352),
                                    RoundedCornerShape(10.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {

    val primaryColor = Color(0xFFB63352)

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            focusedLabelColor = primaryColor
        ),
        label = {
            Text(label)
        },

        // 🔥 penting
        singleLine = true,
        maxLines = 1,

        shape = RoundedCornerShape(5.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropdown(
    label: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {

        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),

            label = {
                Text(label)
            },

            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },

            shape = RoundedCornerShape(5.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            items.forEach { item ->

                DropdownMenuItem(
                    text = {
                        Text(item)
                    },

                    onClick = {

                        onItemSelected(item)
                        expanded = false

                    }
                )
            }
        }
    }
}