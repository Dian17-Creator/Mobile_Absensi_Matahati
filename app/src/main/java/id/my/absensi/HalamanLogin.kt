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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current as ComponentActivity
    val primaryColor = Color(0xFFFF6F51)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // keyboard safe
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // ✅ Proporsional image size berdasarkan tinggi layar
        val imageSize = when {
            screenHeight < 600.dp -> screenHeight * 0.4f   // layar kecil (720p)
            screenHeight < 900.dp -> screenHeight * 0.45f  // layar sedang (1080p)
            else -> screenHeight * 0.5f                    // layar besar (>=1920)
        }

        val textFieldSpacing = screenHeight * 0.02f
        val buttonHeight = screenHeight * 0.07f
        val horizontalPadding = screenWidth * 0.08f

        Scaffold(
            modifier = Modifier.fillMaxSize()
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
                        .verticalScroll(rememberScrollState()) // bisa di-scroll saat keyboard muncul
                        .padding(horizontal = horizontalPadding)
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 🔹 Gambar login proporsional
                    Image(
                        painter = painterResource(id = R.drawable.loginbro),
                        contentDescription = "Login",
                        modifier = Modifier
                            .size(imageSize)
                            .padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(textFieldSpacing))

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
                        keyboardActions = KeyboardActions(onNext = {
                            focusManager.moveFocus(FocusDirection.Down)
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(textFieldSpacing))

                    // 🔹 Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image =
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    image,
                                    contentDescription = if (passwordVisible)
                                        "Sembunyikan Password" else "Tampilkan Password"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(textFieldSpacing))

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

                    Spacer(modifier = Modifier.height(textFieldSpacing))

                    // 🔹 Tombol Login tetap terlihat di atas keyboard
                    Button(
                        onClick = {
                            loginUser(context, username, password) { success, msg, userJson ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

                                if (success && userJson != null) {
                                    val userId = userJson.optString("id")?.toIntOrNull() ?: -1
                                    val userName = userJson.optString("name", "")
                                    val userEmail = userJson.optString("email", "")

                                    val session = SessionManager(context)
                                    session.clearSession()

                                    if (rememberMe) {
                                        session.saveUser(id = userId, name = userName, email = userEmail)
                                        session.setRememberMe(true)
                                    } else {
                                        session.setRememberMe(false)
                                    }

                                    val intent = Intent(context, HalamanScan::class.java)
                                    intent.putExtra("USER_ID", userId)
                                    intent.putExtra("USER_NAME", userName)
                                    intent.putExtra("USER_EMAIL", userEmail)
                                    context.startActivity(intent)
                                    (context as ComponentActivity).finish()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                            .imePadding(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Login")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}