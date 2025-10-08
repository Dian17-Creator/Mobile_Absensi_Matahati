package id.my.matahati.absensi.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import id.my.matahati.absensi.HalamanScanUI

class HomeFragment : Fragment() {

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val hasPermissions = REQUIRED_PERMISSIONS.all {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        it
                    ) == PackageManager.PERMISSION_GRANTED
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
}
