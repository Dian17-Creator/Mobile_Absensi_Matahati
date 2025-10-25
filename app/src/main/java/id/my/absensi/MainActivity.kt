package id.my.matahati.absensi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import id.my.matahati.absensi.fragment.HomeFragment
import id.my.matahati.absensi.fragment.JadwalFragment
import id.my.matahati.absensi.fragment.PasswordFragment
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var lastSelectedItemId: Int = R.id.nav_home

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(applicationContext)

        val isLoggedIn = session.isLoggedIn()
        val rememberMe = session.isRememberMe()
        val sessionPrefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val email = sessionPrefs.getString("email", null)
        Log.d("SESSION_DEBUG", "Email in session: $email")


        // 🔹 Kalau belum login → pindah ke Login
        if (!isLoggedIn) {
            startActivity(Intent(this, HalamanLogin::class.java))
            finish()
            return
        }

        // 🔹 Kalau tidak centang "ingatkan saya", hapus session saat app dibuka ulang
        if (!rememberMe) {
            session.clearSession()
        }

        setContentView(R.layout.activity_main)

        // 🔹 Minta izin kamera & lokasi saat app dibuka
        checkAndRequestPermissions()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        replaceFragment(HomeFragment()) // default fragment

        bottomNav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_SELECTED

        // Atur skala awal untuk animasi ikon
        for (i in 0 until bottomNav.menu.size()) {
            val menuItem = bottomNav.menu.getItem(i)
            val itemView = bottomNav.findViewById<View>(menuItem.itemId)
            val icon = itemView?.findViewById<View>(com.google.android.material.R.id.icon)

            if (menuItem.itemId == bottomNav.selectedItemId) {
                icon?.scaleX = 1.25f
                icon?.scaleY = 1.25f
                lastSelectedItemId = menuItem.itemId
            } else {
                icon?.scaleX = 1f
                icon?.scaleY = 1f
            }
        }

        // 🔹 Listener BottomNavigation
        bottomNav.setOnItemSelectedListener { item ->
            val selectedView = bottomNav.findViewById<View>(item.itemId)
            val selectedIcon = selectedView?.findViewById<View>(com.google.android.material.R.id.icon)

            selectedIcon?.let {
                ObjectAnimator.ofPropertyValuesHolder(
                    it,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.25f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.25f)
                ).apply {
                    duration = 180
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }

            if (lastSelectedItemId != item.itemId) {
                val prevView = bottomNav.findViewById<View>(lastSelectedItemId)
                val prevIcon = prevView?.findViewById<View>(com.google.android.material.R.id.icon)
                prevIcon?.let {
                    ObjectAnimator.ofPropertyValuesHolder(
                        it,
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
                    ).apply {
                        duration = 180
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                }
            }

            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_jadwal -> replaceFragment(JadwalFragment())
                R.id.nav_password -> replaceFragment(PasswordFragment())
            }

            lastSelectedItemId = item.itemId
            true
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (!allGranted) {
                Toast.makeText(
                    this,
                    "Izin kamera dan lokasi diperlukan agar aplikasi berjalan dengan baik.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Izin diberikan ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        for (frag in supportFragmentManager.fragments) {
            transaction.setMaxLifecycle(frag, Lifecycle.State.STARTED)
            transaction.hide(frag)
        }

        val existing = supportFragmentManager.fragments.find { it::class == fragment::class }
        if (existing != null) {
            transaction.setMaxLifecycle(existing, Lifecycle.State.RESUMED)
            transaction.show(existing)
        } else {
            transaction.add(R.id.fragment_container, fragment)
            transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
        }
        transaction.commit()
    }

    // 🔹 Bersihkan cache otomatis saat aplikasi ditutup
    override fun onStop() {
        super.onStop()
        clearAppCache()
    }

    private fun clearAppCache() {
        try {
            val cacheDir: File = cacheDir
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            val externalCache: File? = externalCacheDir
            externalCache?.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
