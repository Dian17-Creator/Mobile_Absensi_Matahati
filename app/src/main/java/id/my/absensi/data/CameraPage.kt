package id.my.absensi.ui

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraPage(
    onPhotoCaptured: (File) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = Executors.newSingleThreadExecutor()
    val previewView = remember { PreviewView(context) }

    val imageCapture = remember {
        androidx.camera.core.ImageCapture.Builder()
            .setCaptureMode(androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var photoFile by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 📷 Preview kamera
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)

        )

        LaunchedEffect(Unit) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = androidx.camera.core.Preview.Builder().build()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraPage", "Gagal inisialisasi kamera depan", e)
            }
        }

        // 📸 Jika sudah ambil foto, tampilkan preview hasilnya
        photoFile?.let {
            Image(
                painter = rememberAsyncImagePainter(Uri.fromFile(it)),
                contentDescription = "Hasil foto",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🔘 Tombol kontrol kamera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (photoFile == null) {
                Button(
                    onClick = {
                        val file = File(context.cacheDir, "front_${System.currentTimeMillis()}.jpg")
                        val outputOptions =
                            androidx.camera.core.ImageCapture.OutputFileOptions.Builder(file).build()

                        imageCapture.takePicture(
                            outputOptions,
                            executor,
                            object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {
                                    photoFile = file
                                }

                                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                    Log.e("CameraPage", "Gagal ambil foto", exception)
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB63352))
                ) {
                    Text("Ambil Foto")
                }
            } else {
                Button(
                    onClick = { photoFile = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Ambil Ulang")
                }

                Button(
                    onClick = {
                        photoFile?.let { onPhotoCaptured(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Gunakan Foto")
                }
            }
        }

        TextButton(onClick = { onCancel() }) {
            Text("Batal", color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
