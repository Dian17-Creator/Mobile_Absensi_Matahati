package id.my.matahati.absensi.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import id.my.matahati.absensi.HalamanJadwalUI
import id.my.matahati.absensi.SessionManager

class JadwalFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext().applicationContext
        val session = SessionManager(context)
        val activity = requireActivity()

        // 🔹 Ambil userId dari session jika tersedia
        var userId = session.getUserId()

        // 🔹 Kalau session kosong, ambil dari intent
        if (userId == -1) {
            userId = activity.intent?.getIntExtra("USER_ID", -1) ?: -1
        }

        // 🔹 Logika tambahan: kalau session kosong tapi rememberMe aktif,
        //    berarti session belum ter-inisialisasi di runtime → ambil ulang dari prefs
        if (userId == -1 && session.isRememberMe()) {
            val user = session.getUser()
            userId = user["id"] as? Int ?: -1
        }

        // 🔹 Jika userId tetap -1, artinya benar-benar belum login
        if (userId == -1) {
            // Bisa tambahkan mekanisme redirect ke login di sini jika mau
        }

        // 🔹 Panggil UI jadwal
        return ComposeView(requireContext()).apply {
            setContent {
                HalamanJadwalUI()
            }
        }
    }
}
