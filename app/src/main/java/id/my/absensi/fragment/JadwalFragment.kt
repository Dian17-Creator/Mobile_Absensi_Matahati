package id.my.matahati.absensi.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.matahati.absensi.HalamanJadwalUI
import id.my.matahati.absensi.SessionManager
import id.my.matahati.absensi.data.ScheduleViewModel
import id.my.matahati.absensi.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JadwalFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext().applicationContext
        val session = SessionManager(context)
        val activity = requireActivity()

        // 🔹 Ambil userId dari session atau intent
        var userId = session.getUserId()
        if (userId == -1) {
            userId = activity.intent?.getIntExtra("USER_ID", -1) ?: -1
        }

        // 🔹 Coba fallback jika rememberMe aktif
        if (userId == -1 && session.isRememberMe()) {
            val user = session.getUser()
            userId = user["id"] as? Int ?: -1
        }

        // 🔹 Jika userId tetap -1, tampilkan info agar tidak error
        if (userId == -1) {
            return ComposeView(requireContext()).apply {
                setContent {
                    androidx.compose.material3.Text(
                        text = "⚠️ Silakan login terlebih dahulu untuk melihat jadwal",
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .wrapContentSize(),
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            }
        }

        // ✅ Kembalikan ComposeView dengan logika online/offline
        return ComposeView(requireContext()).apply {
            setContent {
                val scheduleViewModel: ScheduleViewModel = viewModel()
                val ctx = requireContext()

                // 🔹 Jalankan efek Compose hanya sekali saat fragment aktif
                LaunchedEffect(Unit) {
                    // 1️⃣ Load data dari Room dulu (agar offline langsung muncul)
                    scheduleViewModel.loadSchedules(userId)

                    // 2️⃣ Pantau perubahan koneksi jaringan (sinkron otomatis)
                    withContext(Dispatchers.IO) {
                        NetworkUtils.getNetworkStatusFlow(ctx).collect { isOnline ->
                            if (isOnline) {
                                // 🔄 Jika koneksi kembali aktif, sync ulang jadwal
                                scheduleViewModel.loadSchedules(userId)
                            }
                        }
                    }
                }

                // 🔹 Panggil UI jadwal
                HalamanJadwalUI()
            }
        }
    }
}
