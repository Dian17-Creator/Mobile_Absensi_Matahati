package id.my.matahati.absensi.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import id.my.matahati.absensi.HalamanScanUI
import id.my.matahati.absensi.SessionManager
import id.my.matahati.absensi.data.ScanResult
import id.my.matahati.absensi.data.ScheduleViewModel

class HomeFragment : Fragment() {

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val currentScanResult = mutableStateOf<ScanResult?>(null)
    private var hasHandledSync = false // ✅ Mencegah trigger berulang

    private lateinit var scheduleViewModel: ScheduleViewModel
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        scheduleViewModel = ViewModelProvider(requireActivity())[ScheduleViewModel::class.java]
        session = SessionManager(requireContext())

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
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // ✅ Cek status reconnect dari session
        val savedScanState = session.getScanState()
        if (savedScanState == "waiting_offline") {
            currentScanResult.value = ScanResult.WaitingImage
        } else if (currentScanResult.value == null ||
            currentScanResult.value is ScanResult.SuccessImage) {
            currentScanResult.value = ScanResult.Message("Arahkan kamera ke QR Code")
        }

        hasHandledSync = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val workManager = WorkManager.getInstance(requireContext())

        workManager.getWorkInfosForUniqueWorkLiveData("sync_offline_scans")
            .observe(viewLifecycleOwner, Observer { workInfos ->
                val successWork = workInfos.find { it.state == WorkInfo.State.SUCCEEDED }

                if (successWork != null && !hasHandledSync) {
                    hasHandledSync = true
                    Toast.makeText(
                        requireContext(),
                        "✅ Data berhasil disinkronkan",
                        Toast.LENGTH_SHORT
                    ).show()

                    // ✅ Ubah tampilan menjadi sukses
                    currentScanResult.value = ScanResult.SuccessImage

                    // ✅ Refresh jadwal shift setelah sync berhasil
                    val userId = session.getUserId()
                    if (userId != -1) {
                        scheduleViewModel.loadSchedules(userId)
                    }

                    // ✅ Setelah beberapa detik, reset ke kamera
                    Handler(Looper.getMainLooper()).postDelayed({
                        currentScanResult.value = ScanResult.Message("Arahkan kamera ke QR Code")
                    }, 2500)

                    // 🔹 Clear hasil sukses agar tidak trigger lagi
                    WorkManager.getInstance(requireContext()).pruneWork()
                }
            })
    }
}
