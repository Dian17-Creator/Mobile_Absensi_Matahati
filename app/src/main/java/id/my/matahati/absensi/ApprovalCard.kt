package id.my.matahati.absensi

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.matahati.absensi.data.ApprovalItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@Composable
fun ApprovalCard(
    item: ApprovalItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val primaryColor = Color(0xFFB63352)
    var showImageDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White // 🔥 putih bersih
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        shape = RoundedCornerShape(16.dp) // 🔥 lebih smooth
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                val dateParts = item.tanggal.split(" ")
                val datePart = dateParts.getOrNull(0) ?: item.tanggal
                val timePart = dateParts.getOrNull(1)?.let { if (it.length >= 5) it.substring(0, 5) else it } ?: ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = item.user_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = Color(0xFFF7F7F7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = datePart,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                            if (timePart.isNotBlank()) {
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = timePart,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB63352)
                                )
                            }
                        }
                    }
                }

                if (!item.tempat.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lokasi : ${item.tempat}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 🧠 Keterangan
                Text(
                    text = item.creason ?: "-",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 🧠 Garis
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 🧠 Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC60000)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reject")
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009536)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Approve")
                    }

                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Surface(
                    onClick = {
                        if (item.cphoto_url != null) {
                            showImageDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.LightGray,
                    shape = MaterialTheme.shapes.medium
                ) {
                    val context = LocalContext.current

                    if (item.cphoto_url != null) {

                        Log.d("IMAGE_URL", item.cphoto_url)

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.cphoto_url)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,

                            onError = {
                                Log.e("IMAGE_ERROR", it.result.throwable.toString())
                            },

                            onSuccess = {
                                Log.d("IMAGE_SUCCESS", "Image loaded")
                            }
                        )

                    } else {
                        Log.e("IMAGE_NULL", "URL NULL")

                        Box(contentAlignment = Alignment.Center) {
                            Text("No Image", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showImageDialog && item.cphoto_url != null) {
        Dialog(onDismissRequest = { showImageDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Foto Absen",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.cphoto_url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Zoomed Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        IconButton(
                            onClick = { showImageDialog = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun ApprovalCardPreview() {
    val sampleItem = ApprovalItem(
        nid = 1,
        user_name = "Ahmad Fatih",
        department = "IT Engineering",
        tanggal = "2026-09-08 08:30:00",
        tempat = "Kantor Pusat Jakarta",
        creason = "Sakit Demam, mohon izin istirahat.",
        cphoto_url = null
    )
    Surface(modifier = Modifier.padding(16.dp)) {
        ApprovalCard(
            item = sampleItem,
            onApprove = {},
            onReject = {}
        )
    }
}
