package id.my.matahati.absensi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import id.my.matahati.absensi.data.RetrofitClientLaravel
import id.my.matahati.absensi.data.CompanyCheckResponse
import id.my.matahati.absensi.data.RegisterRequest
import kotlinx.coroutines.launch

class HalamanRegister : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RegisterUI()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterUI() {

    var company by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var companyExists by remember {
        mutableStateOf<Boolean?>(null)
    }

    var checkingCompany by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current as ComponentActivity
    val focusManager = LocalFocusManager.current

    val primaryColor = Color(0xFFB63352)

    val email by remember(company, name) {
        derivedStateOf {

            val domain = company
                .lowercase()
                .replace(Regex("^(pt|cv)\\s+"), "")
                .replace(Regex("[^a-z0-9]"), "")

            val username = name
                .lowercase()
                .replace(Regex("[^a-z0-9]"), "")

            if (username.isBlank() || domain.isBlank())
                ""
            else
                "$username@$domain"
        }
    }

    LaunchedEffect(company) {

        companyExists = null

        if (company.length < 2)
            return@LaunchedEffect

        delay(500)

        checkingCompany = true

        try {

            val response =
                RetrofitClientLaravel.instance.checkCompany(company)

            if (response.isSuccessful) {
                companyExists = response.body()?.exists
            }

        } catch (e: Exception) {

            companyExists = null

        } finally {

            checkingCompany = false

        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(R.drawable.register),
                contentDescription = null,
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Nama Perusahaan") },
                placeholder = { Text("Contoh: Matahati") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                    }
                ),

                supportingText = {

                    when {

                        checkingCompany -> {
                            Text(
                                text = "Memeriksa nama perusahaan...",
                                color = Color.Gray
                            )
                        }

                        companyExists == true -> {
                            Text(
                                text = "❌ Nama perusahaan sudah terdaftar",
                                color = Color.Red
                            )
                        }

                        companyExists == false -> {
                            Text(
                                text = "✅ Nama perusahaan tersedia",
                                color = Color(0xFF2E7D32)
                            )
                        }

                    }

                },

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation =
                    if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                trailingIcon = {

                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {

                        Icon(
                            if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor
                )
            )

            Text(
                text = "Minimal 6 karakter",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                enabled = companyExists == false && !loading,
                onClick = {
                    handleRegister(
                        context,
                        company,
                        name,
                        password
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {

                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("DAFTAR")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    "Sudah punya akun?",
                    color = Color.Gray
                )

                TextButton(
                    onClick = {
                        context.finish()
                    }
                ) {

                    Text(
                        "Login",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun handleRegister(
    context: ComponentActivity,
    company: String,
    name: String,
    password: String
) {

    if (company.isBlank()) {
        Toast.makeText(context, "Nama perusahaan wajib diisi", Toast.LENGTH_SHORT).show()
        return
    }

    if (name.isBlank()) {
        Toast.makeText(context, "Nama wajib diisi", Toast.LENGTH_SHORT).show()
        return
    }

    if (password.length < 6) {
        Toast.makeText(context, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
        return
    }

    context.lifecycleScope.launch {

        try {

            val response = RetrofitClientLaravel.instance.register(
                RegisterRequest(
                    ccompany = company,
                    cname = name,
                    cpassword = password
                )
            )

            if (response.isSuccessful) {

                val body = response.body()

                if (body != null && body.success) {

                    Toast.makeText(
                        context,
                        body.message,
                        Toast.LENGTH_LONG
                    ).show()

                    context.finish()

                } else {

                    Toast.makeText(
                        context,
                        "Registrasi gagal",
                        Toast.LENGTH_LONG
                    ).show()

                }

            }

        } catch (e: Exception) {

            Toast.makeText(
                context,
                e.localizedMessage ?: "Terjadi kesalahan",
                Toast.LENGTH_LONG
            ).show()

        }

    }

}