package id.my.matahati.absensi

import okhttp3.*
import android.os.Bundle
import org.json.JSONObject
import java.io.IOException
import android.widget.Toast
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.OutlinedTextField
// handle enter untuk inputan
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions

class HalamanLogin : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginUI()
        }
    }
}

// 🔹 Fungsi API login
fun loginUser(
    context: ComponentActivity,
    email: String,
    password: String,
    onResult: (Boolean, String, JSONObject?) -> Unit
) {
    val client = OkHttpClient()
    val url = "https://absensi.matahati.my.id/login.php?api=1"

    val formBody = FormBody.Builder()
        .add("email", email)
        .add("password", password)
        .build()

    val request = Request.Builder()
        .url(url)
        .post(formBody)
        .addHeader("Accept", "application/json")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            context.runOnUiThread {
                onResult(false, "Network error: ${e.message}", null)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                val bodyString = response.body?.string() ?: ""
                try {
                    val json = JSONObject(bodyString)
                    if (json.getBoolean("success")) {
                        val user = json.getJSONObject("user")
                        context.runOnUiThread {
                            onResult(true, "Selamat Datang ${user.getString("name")}", user)
                        }
                    } else {
                        context.runOnUiThread {
                            onResult(false, json.getString("message"), null)
                        }
                    }
                } catch (e: Exception) {
                    context.runOnUiThread {
                        onResult(false, "Invalid response: $bodyString", null)
                    }
                }
            }
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginUI() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current  // focus inputan

    val context = LocalContext.current as ComponentActivity
    val primaryColor = Color(0xFFFF6F51) // oranye custom

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.loginbro),
                    contentDescription = "Login",
                    modifier = Modifier.size(330.dp)
                )

                // 🔹 Email
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Email") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                image,
                                contentDescription = if (passwordVisible) "Sembunyikan Password" else "Tampilkan Password"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Checkbox + Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = primaryColor,
                            uncheckedColor = primaryColor,
                            checkmarkColor = Color.White
                        )
                    )
                    Text("Ingatkan saya")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 🔹 Tombol Login
                Button(
                    onClick = {
                        loginUser(context, username, password) { success, msg, userJson ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success && userJson != null) {
                                if (rememberMe) {
                                    val session = SessionManager(context)
                                    session.clearSession() // Hapus session lama

                                    val userId = userJson.optString("id")?.toIntOrNull() ?: -1
                                    val userName = userJson.optString("name", "")
                                    val userEmail = userJson.optString("email", "")

                                    session.saveUser(
                                        id = userId,
                                        name = userName,
                                        email = userEmail
                                    )
                                }

                                val intent = Intent(context, HalamanScan::class.java)
                                context.startActivity(intent)
                                (context as ComponentActivity).finish()
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
                    Text("Login")
                }
            }
        }
    }
}
