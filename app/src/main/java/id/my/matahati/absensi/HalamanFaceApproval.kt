package id.my.matahati.absensi

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import id.my.matahati.absensi.data.FaceApprovalViewModel

class HalamanFaceApproval : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    val primaryColor = Color(0xFFB63352)

    LaunchedEffect(Unit) {
        viewModel.loadPending(approverId)
    }

    val users = viewModel.users
    val loading = viewModel.loading

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        /* ================= HEADER BACKGROUND ================= */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(
                    BottomCurveShape(
                        curveHeight = 50f
                    )
                )
                .background(primaryColor)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp)
        ) {

            /* ================= TITLE ================= */
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {

                IconButton(
                    onClick = { (context as Activity).finish() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFFFFFFF))
                }

                Text(
                    text = "Face Approval",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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

                users.isEmpty() -> {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "Belum ada face approval pending",
                            modifier = Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 18.dp
                            ),
                            color = Color.Black
                        )
                    }
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(
                            bottom = 24.dp
                        )
                    ) {

                        items(users) { user ->

                            Card(
                                modifier = Modifier.fillMaxWidth(),

                                shape = RoundedCornerShape(22.dp),

                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 10.dp
                                ),

                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {

                                    /* ================= USER INFO ================= */

                                    Text(
                                        text = "${user.cname} | ${user.department ?: "-"}",

                                        fontSize = 16.sp,

                                        fontWeight = FontWeight.Normal,

                                        color = Color.Black
                                    )

                                    Spacer(
                                        modifier = Modifier.height(12.dp)
                                    )

                                    /* ================= FACE LIST ================= */

                                    LazyRow(
                                        horizontalArrangement =
                                            Arrangement.spacedBy(10.dp)
                                    ) {

                                        items(user.faces) { face ->

                                            Card(
                                                shape = RoundedCornerShape(16.dp),
                                                elevation =
                                                    CardDefaults.cardElevation(
                                                        defaultElevation = 6.dp
                                                    )
                                            ) {

                                                AsyncImage(
                                                    model = face.url,

                                                    contentDescription = null,

                                                    modifier = Modifier
                                                        .size(94.dp),

                                                    contentScale =
                                                        ContentScale.Crop
                                                )
                                            }
                                        }
                                    }

                                    Spacer(
                                        modifier = Modifier.height(16.dp)
                                    )

                                    /* ================= BUTTON ================= */

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(12.dp)
                                    ) {

                                        /* ===== REJECT ===== */

                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),

                                            onClick = {

                                                viewModel.reject(
                                                    user.nid,
                                                    approverId
                                                ) {

                                                    viewModel.loadPending(
                                                        approverId
                                                    )
                                                }
                                            },

                                            shape = RoundedCornerShape(25),

                                            colors =
                                                ButtonDefaults.outlinedButtonColors(
                                                    contentColor = primaryColor
                                                )
                                        ) {

                                            Text(
                                                text = "Reject",
                                                fontWeight =
                                                    FontWeight.SemiBold
                                            )
                                        }

                                        /* ===== APPROVE ===== */

                                        Button(
                                            modifier = Modifier.weight(1f),

                                            onClick = {

                                                viewModel.approve(
                                                    user.nid,
                                                    approverId
                                                ) {

                                                    viewModel.loadPending(
                                                        approverId
                                                    )
                                                }
                                            },

                                            shape = RoundedCornerShape(25),

                                            colors =
                                                ButtonDefaults.buttonColors(
                                                    containerColor =
                                                        primaryColor,
                                                    contentColor =
                                                        Color.White
                                                )
                                        ) {

                                            Text(
                                                text = "Approve",
                                                fontWeight =
                                                    FontWeight.SemiBold
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
}