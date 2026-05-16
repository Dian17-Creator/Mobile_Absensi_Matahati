@file:OptIn(ExperimentalMaterial3Api::class)

package id.my.matahati.absensi

import android.app.Activity
import android.os.Bundle
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

    val primaryColor = Color(0xFF198754)

    var username by remember { mutableStateOf("") }
    var gmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var telepon by remember { mutableStateOf("") }
    var ktp by remember { mutableStateOf("") }
    var fingerId by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var namaLengkap by remember { mutableStateOf("") }
    var rekening by remember { mutableStateOf("") }

    // ======================
// STATE
// ======================

    var tanggalMasuk by remember { mutableStateOf("") }

    var selectedRole by remember {
        mutableStateOf("Crew")
    }

// DATE PICKER
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember {
        mutableStateOf(false)
    }


// ======================
// DATE PICKER DIALOG
// ======================

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
                                "dd-MM-yyyy",
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
                .height(420.dp)
                .align(Alignment.BottomCenter)
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
                painter = painterResource(id = R.drawable.attendance),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CARD FORM
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    CustomField("Username", username) {
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
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomField("Nomor Rekening", rekening) {
                        rekening = it
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DummyDropdown("Jenis Bank")

                    Spacer(modifier = Modifier.height(12.dp))

                    DummyDropdown("Departemen")

                    Spacer(modifier = Modifier.height(12.dp))

                    DummyDropdown("Payroll Department")

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
                                    .padding(vertical = 4.dp),
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
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4C4C59)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Text(
                            text = "Simpan",
                            color = Color.White,
                            fontSize = 16.sp
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
    onChange: (String) -> Unit
) {

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun DummyDropdown(
    label: String
) {

    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = {
            Text(label)
        },
        placeholder = {
            Text("-- Pilih --")
        },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun RoleCircle(
    text: String,
    selected: Boolean = false
) {

    Surface(
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color =
            if (selected) Color.DarkGray
            else Color.LightGray
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color =
                    if (selected) Color.White
                    else Color.Black
            )
        }
    }
}