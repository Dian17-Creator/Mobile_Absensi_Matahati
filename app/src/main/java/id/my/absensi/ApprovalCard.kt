package id.my.matahati.absensi

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.matahati.absensi.data.ApprovalItem

@Composable
fun ApprovalCard(
    item: ApprovalItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.user_name, fontWeight = FontWeight.Bold)
            Text(item.creason ?: "-", fontSize = 12.sp, color = Color.Gray)
            Text(item.tanggal, fontSize = 11.sp, color = Color.Gray)

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Approve")
                }
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Reject")
                }
            }
        }
    }
}
