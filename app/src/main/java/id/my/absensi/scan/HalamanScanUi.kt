package id.my.matahati.absensi.tampilan.scan

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.matahati.absensi.ScanResult
import id.my.matahati.absensi.UbahPassword
import id.my.matahati.absensi.LogoutHelper
import id.my.matahati.absensi.data.sendToVerify
import androidx.compose.foundation.shape.RoundedCornerShape


@Composable
fun HalamanScanUI(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var scanResult by remember { mutableStateOf<ScanResult>(ScanResult.Message("Arahkan kamera ke QR Code")) }
    var showCamera by remember { mutableStateOf(true) }

    val primaryColor = Color(0xFFFF6F51)

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Silahkan Scan Barcode",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 32.dp)
                )

                if (hasCameraPermission && showCamera) {
                    CameraPreviewBox(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)   // ❗ lebarnya 90% dari layar
                            .height(450.dp),      // ❗ tinggi 400dp
                        onScan = { token ->
                            showCamera = false
                            sendToVerify(context, token) { result ->
                                scanResult = result
                            }
                        }
                    )

                } else if (!hasCameraPermission) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Izin kamera/lokasi belum diberikan")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onRequestPermission() }) {
                            Text("Berikan Izin")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // tampilkan hasil scan
                when (scanResult) {
                    is ScanResult.Message -> Text(
                        text = (scanResult as ScanResult.Message).text,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    is ScanResult.WaitingImage -> WaitingPreview()
                    is ScanResult.SuccessImage -> SuccessView()
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(context, UbahPassword::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ubah Password")
                    }

                    Button(
                        onClick = { LogoutHelper.logout(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
    }
}
