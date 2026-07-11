package id.my.matahati.absensi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.matahati.absensi.data.PayrollViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

class HalamanPayroll : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val topPadding =
                WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()

            HalamanPayrollScreen(topPadding)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalamanPayrollScreen(
    topPadding : Dp,
    viewModel: PayrollViewModel = viewModel()
) {

    val context = LocalContext.current

    val payrolls = viewModel.payrolls
    val loading = viewModel.loading

    val primaryColor = Color(0xFFB63352)

    LaunchedEffect(Unit) {
        viewModel.loadDepartments()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        /* ================= HEADER ================= */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topPadding)
                .background(Color.Black)
                .align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .height(450.dp)
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


                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { (context as Activity).finish() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFFFFFFF))
                    }


                    Text(
                        text = "Payroll User",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

            Spacer(modifier = Modifier.height(24.dp))

            /* ================= FILTER ================= */

            val departments = viewModel.departments

            val years = (2024..2030).toList()

            val months = listOf(
                1,2,3,4,5,6,7,8,9,10,11,12
            )

            var expandedDept by remember {
                mutableStateOf(false)
            }

            var expandedYear by remember {
                mutableStateOf(false)
            }

            var expandedMonth by remember {
                mutableStateOf(false)
            }

            Card(
                shape = RoundedCornerShape(16.dp),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                ),

                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

//                    Text(
//                        text = "Pilih Periode Payroll",
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        modifier = Modifier
//                            .fillMaxWidth(),
//                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
//                    )

//                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        //Searching
                        OutlinedTextField(

                            value = viewModel.searchQuery,

                            onValueChange = {

                                viewModel.searchQuery = it
                            },

                            modifier = Modifier.fillMaxWidth(),

                            singleLine = true,

                            shape = RoundedCornerShape(50.dp),

                            placeholder = {

                                Text(
                                    text = "Cari Nama User",
                                    color = Color.Gray
                                )
                            },

                            trailingIcon = {

                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            },

                            colors = OutlinedTextFieldDefaults.colors(

                                focusedBorderColor = primaryColor,

                                unfocusedBorderColor = Color.LightGray,

                                cursorColor = primaryColor
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        /* ================= DEPARTMENT ================= */

                        ExposedDropdownMenuBox(

                            expanded = expandedDept,

                            onExpandedChange = {
                                expandedDept = !expandedDept
                            }
                        ) {

                            OutlinedTextField(

                                value =
                                    departments
                                        .find {
                                            it.nid.toString() ==
                                                    viewModel.selectedDepartment
                                        }
                                        ?.let {
                                            it.code ?: it.cname
                                        } ?: "",

                                onValueChange = {},

                                readOnly = true,

                                singleLine = true,

                                label = {
                                    Text("Pilih Department")
                                },

                                trailingIcon = {

                                    ExposedDropdownMenuDefaults
                                        .TrailingIcon(
                                            expanded = expandedDept
                                        )
                                },

                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    focusedLabelColor = primaryColor
                                )
                            )

                            ExposedDropdownMenu(

                                expanded = expandedDept,

                                onDismissRequest = {
                                    expandedDept = false
                                }
                            ) {

                                DropdownMenuItem(

                                    text = {
                                        Text("Tampilkan Semua")
                                    },

                                    onClick = {

                                        viewModel.setDepartment(null)

                                        expandedDept = false
                                    }
                                )

                                departments.forEach { dept ->

                                    DropdownMenuItem(

                                        text = {

                                            Text(
                                                text =
                                                    dept.code ?: dept.cname
                                            )
                                        },

                                        onClick = {

                                            viewModel.setDepartment(
                                                dept.nid.toString()
                                            )

                                            expandedDept = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        /* ================= YEAR + MONTH ================= */

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            /* ================= YEAR ================= */

                            ExposedDropdownMenuBox(

                                expanded = expandedYear,

                                onExpandedChange = {
                                    expandedYear = !expandedYear
                                },

                                modifier = Modifier.weight(1f)
                            ) {

                                OutlinedTextField(

                                    value =
                                        viewModel.selectedYear.toString(),

                                    onValueChange = {},

                                    readOnly = true,

                                    singleLine = true,

                                    label = {
                                        Text("Pilih Tahun")
                                    },

                                    trailingIcon = {

                                        ExposedDropdownMenuDefaults
                                            .TrailingIcon(
                                                expanded = expandedYear
                                            )
                                    },

                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        focusedLabelColor = primaryColor
                                    )
                                )

                                ExposedDropdownMenu(

                                    expanded = expandedYear,

                                    onDismissRequest = {
                                        expandedYear = false
                                    }
                                ) {

                                    years.forEach { year ->

                                        DropdownMenuItem(

                                            text = {
                                                Text(year.toString())
                                            },

                                            onClick = {

                                                viewModel.setYear(year)

                                                expandedYear = false
                                            }
                                        )
                                    }
                                }
                            }

                            /* ================= MONTH ================= */

                            ExposedDropdownMenuBox(

                                expanded = expandedMonth,

                                onExpandedChange = {
                                    expandedMonth = !expandedMonth
                                },

                                modifier = Modifier.weight(1f)
                            ) {

                                OutlinedTextField(

                                    value =
                                        viewModel.selectedMonth.toString(),

                                    onValueChange = {},

                                    readOnly = true,

                                    singleLine = true,

                                    label = {
                                        Text("Pilih Bulan")
                                    },

                                    trailingIcon = {

                                        ExposedDropdownMenuDefaults
                                            .TrailingIcon(
                                                expanded = expandedMonth
                                            )
                                    },

                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        focusedLabelColor = primaryColor
                                    )
                                )

                                ExposedDropdownMenu(

                                    expanded = expandedMonth,

                                    onDismissRequest = {
                                        expandedMonth = false
                                    }
                                ) {

                                    months.forEach { month ->

                                        DropdownMenuItem(

                                            text = {
                                                Text(month.toString())
                                            },

                                            onClick = {

                                                viewModel.setMonth(month)

                                                expandedMonth = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            /* ================= CONTENT ================= */

            when {

                loading -> {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = primaryColor
                        )
                    }
                }

                payrolls.isEmpty() -> {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "Data payroll kosong"
                        )
                    }
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp),
                        contentPadding =
                            PaddingValues(bottom = 24.dp)
                    ) {

                        items(viewModel.filteredPayrolls) { payroll ->

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation =
                                    CardDefaults.cardElevation(
                                        defaultElevation = 8.dp
                                    ),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            Color.White
                                    )
                            ) {

                                Column(
                                    modifier = Modifier.padding(18.dp)
                                ) {

                                    /* ================= NAME ================= */

                                    Text(
                                        text = payroll.user_name ?: "-",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Text(
                                        text = payroll.jabatan ?: "-",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )

                                    Spacer(
                                        modifier = Modifier.height(18.dp)
                                    )

                                    /* ================= PAYROLL INFO ================= */

                                    PayrollInfoItem(
                                        label = "Jumlah Masuk",
                                        value =
                                            payroll.jumlah_masuk.toString()
                                    )

                                    PayrollInfoItem(
                                        label = "Gaji Pokok",
                                        value = payroll.gaji_pokok ?: "-"
                                    )

                                    PayrollInfoItem(
                                        label = "Total Gaji",
                                        value = payroll.total_gaji ?: "-"
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement =
                                            Arrangement.SpaceBetween,

                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text = "Status",
                                            color = Color.Gray
                                        )

                                        StatusButton(
                                            status = payroll.status ?: "pending"
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.height(18.dp)
                                    )

                                    /* ================= BUTTON ================= */

                                    Button(
                                        onClick = {

                                            payroll.id?.let { payrollId ->

                                                val intent = Intent(
                                                    context,
                                                    HalamanEditPayroll::class.java
                                                )

                                                intent.putExtra(
                                                    "payroll_id",
                                                    payrollId
                                                )

                                                context.startActivity(intent)
                                            }
                                        },

                                        modifier = Modifier
                                            .height(50.dp)
                                            .fillMaxWidth(),

                                        shape = RoundedCornerShape(50.dp),

                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor =
                                                    primaryColor
                                            )
                                    ) {

                                        Spacer(
                                            modifier = Modifier.width(8.dp)
                                        )

                                        Text(
                                            text = "Detail Payroll"
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
fun PayrollInfoItem(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            color = Color.Gray
        )

        Text(
            text = value,
            fontWeight = FontWeight.SemiBold
        )
    }
}

