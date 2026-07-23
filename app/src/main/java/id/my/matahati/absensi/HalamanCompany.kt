package id.my.matahati.absensi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.matahati.absensi.data.CompanyViewModel
import kotlinx.coroutines.flow.collectLatest

class HalamanCompany : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: CompanyViewModel = viewModel()
            val context = LocalContext.current
            val sessionManager = SessionManager(context)

            LaunchedEffect(Unit) {
                vm.loadCompany(sessionManager.getUserId())
            }

            LaunchedEffect(Unit) {
                vm.updateSuccess.collectLatest { success ->
                    if (success) {
                        Toast.makeText(context, "Data company berhasil diperbarui.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }

            val primaryColor = Color(0xFFB63352)
            val backColor = Color(0xFFFFF5F5)

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Company",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = primaryColor,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backColor)
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Data Company",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                )
                                Text(
                                    text = "Perbarui informasi nama perusahaan dan domain email yang diizinkan untuk absensi.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }

                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Field Nama Company
                                    OutlinedTextField(
                                        value = vm.companyName,
                                        onValueChange = {
                                            vm.onCompanyNameChange(it)
                                            vm.checkCompany(sessionManager.getUserId())
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Nama Company") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Business, contentDescription = null, tint = primaryColor)
                                        },
                                        trailingIcon = {
                                            if (vm.checking) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = primaryColor
                                                )
                                            }
                                        },
                                        supportingText = {
                                            if (!vm.checking) {
                                                when {
                                                    vm.nameExists -> Text("Nama company sudah digunakan", color = MaterialTheme.colorScheme.error)
                                                    vm.companyName.isNotBlank() -> Text("Nama company tersedia", color = Color(0xFF2E7D32))
                                                }
                                            }
                                        },
                                        isError = vm.nameExists,
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Field Domain Email
                                    OutlinedTextField(
                                        value = vm.companyEmail,
                                        onValueChange = {
                                            vm.onCompanyEmailChange(it)
                                            vm.checkCompany(sessionManager.getUserId())
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Domain Email") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor)
                                        },
                                        trailingIcon = {
                                            if (vm.checking) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = primaryColor
                                                )
                                            }
                                        },
                                        supportingText = {
                                            if (!vm.checking) {
                                                when {
                                                    vm.domainExists -> Text("Domain sudah digunakan", color = MaterialTheme.colorScheme.error)
                                                    vm.companyEmail.isNotBlank() -> Text("Domain tersedia", color = Color(0xFF2E7D32))
                                                }
                                            }
                                        },
                                        isError = vm.domainExists,
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Button Simpan di bawah
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(20.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = { vm.updateCompany(sessionManager.getUserId()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !vm.saving && !vm.nameExists && !vm.domainExists,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            if (vm.saving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Simpan Perubahan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}
