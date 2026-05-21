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

@Composable
fun ApprovalCard(
    item: ApprovalItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val primaryColor = Color(0xFFB63352)

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom // 🔥 ini kunci
                ) {

                    Text(
                        text = item.user_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = " | ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )

                    Text(
                        text = item.tanggal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 2.dp) // 🔥 fine tuning
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
            ) {
                Surface(
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
}
