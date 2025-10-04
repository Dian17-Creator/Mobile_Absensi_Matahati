package id.my.matahati.absensi.tampilan.scan

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import id.my.matahati.absensi.R


@Composable
fun SuccessView() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.good),
            contentDescription = "Scan berhasil",
            modifier = Modifier.size(350.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan berhasil",
            fontSize = 20.sp,
            color = Color(0xFF4CAF50)
        )
    }
}
