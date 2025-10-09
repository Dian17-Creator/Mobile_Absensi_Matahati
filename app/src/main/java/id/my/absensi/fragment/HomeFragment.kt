package id.my.matahati.absensi.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import id.my.matahati.absensi.HalamanScanUI
import id.my.matahati.absensi.data.ScanResult

class HomeFragment : Fragment() {

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val currentScanResult = mutableStateOf<ScanResult?>(null)
    private var hasHandledSync = false // ✅ Flag agar observer hanya jalan sekali

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val hasPermissions = REQUIRED_PERMISSIONS.all {
                    ContextCompat.checkSelfPermission(requireContext(), it) ==
                            PackageManager.PERMISSION_GRANTED
                }

                HalamanScanUI(
                    hasCameraPermission = hasPermissions,
                    onRequestPermission = {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            REQUIRED_PERMISSIONS,
                            100
                        )
                    },
                    externalScanResult = currentScanResult.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 🔹 Reset ke tampilan kamera saat fragment pertama kali dibuka atau setelah pindah halaman
        if (currentScanResult.value == null ||
            currentScanResult.value is ScanResult.SuccessImage) {
            currentScanResult.value = ScanResult.Message("Arahkan kamera ke QR Code")
        }

        hasHandledSync = false // reset supaya bisa deteksi sync berikutnya
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val workManager = WorkManager.getInstance(requireContext())

        workManager.getWorkInfosForUniqueWorkLiveData("sync_offline_scans")
            .observe(viewLifecycleOwner, Observer { workInfos ->
                val successWork = workInfos.find { it.state == WorkInfo.State.SUCCEEDED }

                if (successWork != null && !hasHandledSync) {
                    hasHandledSync = true // ✅ Cegah trigger berulang
                    Toast.makeText(
                        requireContext(),
                        "✅ Data berhasil disinkronkan",
                        Toast.LENGTH_SHORT
                    ).show()

                    currentScanResult.value = ScanResult.SuccessImage

                    // ✅ Setelah beberapa detik, reset ke kamera kembali (optional)
                    Handler(Looper.getMainLooper()).postDelayed({
                        currentScanResult.value = ScanResult.Message("Arahkan kamera ke QR Code")
                    }, 2500)

                    // 🔹 Clear hasil sukses agar tidak terpicu lagi di observer berikutnya
                    WorkManager.getInstance(requireContext()).pruneWork()
                }
            })
    }
}
