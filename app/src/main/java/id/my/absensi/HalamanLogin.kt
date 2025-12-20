package id.my.matahati.absensi

import okhttp3.*
import android.content.Intent
import android.os.Bundle
import org.json.JSONObject
import java.io.IOException
import android.widget.Toast
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity

class HalamanLogin : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginUI()
        }
    }
}

// ✅ fungsi login tetap sama
fun loginUser(
    context: ComponentActivity,
    email: String,
    password: String,
    onResult: (Boolean, String, JSONObject?) -> Unit
) {
    val client = OkHttpClient()
    val url = "https://absensi.matahati.my.id/user_login_mobile.php?api=1"

    val formBody = FormBody.Builder()
        .add("email", email)
        .add("password", password)
        .build()

    val request = Request.Builder()
        .url(url)
        .post(formBody)
        .addHeader("Accept", "application/json")
        .addHeader("X-Requested-With", "XMLHttpRequest") // 🔥 WAJIB
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
    val context = LocalContext.current as ComponentActivity
    val focusManager = LocalFocusManager.current
    val primaryColor = Color(0xFFB63352)
    var anyFocused by remember { mutableStateOf(false) }
    val isKeyboardOpen by keyboardAsState()
    val animatedOffset by animateDpAsState(
        targetValue = if (isKeyboardOpen) (-180).dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "loginSlide"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .offset(y = animatedOffset)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.loginbro),
                contentDescription = "Login",
                modifier = Modifier
                    .size(260.dp)
                    .padding(bottom = 24.dp),
                contentScale = ContentScale.Fit
            )

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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = null)
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
                    handleLogin(context, username, password, rememberMe)
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { anyFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Button(
                onClick = {
                    handleLogin(context, username, password, rememberMe)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("LOGIN")
            }
        }
    }
}

fun handleLogin(
    context: ComponentActivity,
    email: String,
    password: String,
    rememberMe: Boolean
) {
    loginUser(context, email, password) { success, msg, userJson ->
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

        if (success && userJson != null) {

            val userId = userJson.optInt("id", -1)
            val userName = userJson.optString("name", "")
            val userEmail = userJson.optString("email", "")

            // 🔐 AMBIL ROLE DARI API
            val role = userJson.optJSONObject("role")
            val fadmin = role?.optInt("fadmin", 0) ?: 0
            val fsuper = role?.optInt("fsuper", 0) ?: 0
            val fhrd   = role?.optInt("fhrd", 0) ?: 0

            // 🔍 DEBUG (WAJIB)
            android.util.Log.d(
                "ROLE_LOGIN",
                "Login role → admin=$fadmin super=$fsuper hrd=$fhrd"
            )

            val session = SessionManager(context.applicationContext)

            // ✅ SIMPAN ROLE KE SESSION
            session.login(
                id = userId,
                name = userName,
                email = userEmail,
                rememberMe = rememberMe,
                fadmin = fadmin,
                fsuper = fsuper,
                fhrd = fhrd
            )

            // 🔍 DEBUG SESSION
            android.util.Log.d(
                "ROLE_SESSION",
                "Session → admin=${session.isAdmin()} " +
                        "captain=${session.isCaptain()} hrd=${session.isHRD()}"
            )

            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("USER_NAME", userName)
            intent.putExtra("USER_EMAIL", userEmail)
            context.startActivity(intent)
            context.finish()
        }
    }
}

@Composable
fun keyboardAsState(): State<Boolean> {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val isImeVisible = ime.getBottom(density) > 0
    val keyboardState = remember { mutableStateOf(isImeVisible) }

    LaunchedEffect(isImeVisible) {
        keyboardState.value = isImeVisible
    }
    return keyboardState
}
