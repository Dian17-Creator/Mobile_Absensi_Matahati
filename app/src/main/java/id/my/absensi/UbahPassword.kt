package id.my.matahati.absensi

import android.content.Context
import okhttp3.*
import android.os.Bundle
import org.json.JSONObject
import java.io.IOException
import android.widget.Toast
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation

class UbahPassword : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UbahPasswordUI()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbahPasswordUI() {
    val context = LocalContext.current
    val session = SessionManager(context)

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // ✅ state untuk visibility toggle
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFFFF6F51) // oranye custom

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubah Password") },
                navigationIcon = {
                    IconButton(onClick = {
                        // kembali ke HalamanScan
                        val intent = Intent(context, HalamanScan::class.java)
                        context.startActivity(intent)
                        if (context is ComponentActivity) {
                            context.finish()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.password), // ganti sesuai drawable
                    contentDescription = "Dummy Image",
                    modifier = Modifier
                        .size(300.dp)
                        .padding(bottom = 3.dp)
                )
                // 🔹 Password Lama
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Password Lama") },
                    visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (oldPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                            Icon(image, contentDescription = if (oldPasswordVisible) "Sembunyikan" else "Tampilkan")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Password Baru
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Password Baru") },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(image, contentDescription = if (newPasswordVisible) "Sembunyikan" else "Tampilkan")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Konfirmasi Password Baru
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Konfirmasi Password Baru") },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(image, contentDescription = if (confirmPasswordVisible) "Sembunyikan" else "Tampilkan")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "Password baru tidak sama", Toast.LENGTH_LONG).show()
                        } else {
                            val userId = session.getUserId().toString() // ✅ ambil ID dengan benar
                            updatePassword(
                                context,
                                userId,
                                oldPassword,
                                newPassword
                            ) {
                                // ✅ Kembali ke HalamanScan
                                val intent = Intent(context, HalamanScan::class.java)
                                context.startActivity(intent)
                                if (context is ComponentActivity) {
                                    context.finish()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}

// 🔹 Fungsi kirim password ke server
fun updatePassword(
    context: Context,
    userId: String,
    oldPass: String,
    newPass: String,
    onSuccess: () -> Unit
) {
    val client = OkHttpClient()
    val url = "https://absensi.matahati.my.id/change_password.php"

    val json = JSONObject()
    json.put("userId", userId)
    json.put("oldPassword", oldPass)
    json.put("newPassword", newPass)

    val body = json.toString()
        .toRequestBody("application/json; charset=utf-8".toMediaType())

    val request = Request.Builder()
        .url(url)
        .post(body)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            (context as ComponentActivity).runOnUiThread {
                Toast.makeText(context, "❌ Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            val respBody = response.body?.string()
            val msg = try {
                JSONObject(respBody ?: "{}").optString("message", "Unknown response")
            } catch (e: Exception) {
                "Invalid server response: $respBody"
            }

            (context as ComponentActivity).runOnUiThread {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                if (msg.contains("berhasil", ignoreCase = true) ||
                    msg.contains("success", ignoreCase = true)
                ) {
                    onSuccess()
                }
            }
        }
    })
}
