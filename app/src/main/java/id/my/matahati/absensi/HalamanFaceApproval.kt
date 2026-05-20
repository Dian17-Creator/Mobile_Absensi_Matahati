package id.my.matahati.absensi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import id.my.matahati.absensi.data.FaceApprovalViewModel

class HalamanFaceApproval : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                HalamanFaceApprovalScreen()
            }
        }
    }
}

@Composable
fun HalamanFaceApprovalScreen(
    viewModel: FaceApprovalViewModel = viewModel()
) {
    val context = LocalContext.current
    val session = SessionManager(context)

    val approverId = session.getUserId()

    LaunchedEffect(Unit) {
        viewModel.loadPending(approverId)
    }

    val users = viewModel.users
    val loading = viewModel.loading

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (loading) {

            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {

                items(users) { user ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = user.cname,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = user.department ?: "-",
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow {

                                items(user.faces) { face ->

                                    AsyncImage(
                                        model = face.url,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row {

                                Button(
                                    onClick = {
                                        viewModel.approve(
                                            user.nid,
                                            approverId
                                        ) {
                                            viewModel.loadPending(approverId)
                                        }
                                    }
                                ) {
                                    Text("Approve")
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Button(
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red
                                    ),
                                    onClick = {
                                        viewModel.reject(
                                            user.nid,
                                            approverId
                                        ) {
                                            viewModel.loadPending(approverId)
                                        }
                                    }
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}