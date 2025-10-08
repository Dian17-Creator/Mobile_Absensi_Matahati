package id.my.matahati.absensi

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import id.my.matahati.absensi.fragment.HomeFragment
import id.my.matahati.absensi.fragment.JadwalFragment
import id.my.matahati.absensi.fragment.PasswordFragment

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var lastSelectedItemId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(applicationContext)

        val isLoggedIn = session.isLoggedIn()
        val rememberMe = session.isRememberMe()

        // 🔹 Kalau tidak remember dan tidak login → lempar ke Login
        if (!isLoggedIn) {
            startActivity(Intent(this, HalamanLogin::class.java))
            finish()
            return
        }

        // 🔹 Kalau tidak centang "ingatkan saya", reset login ketika app dibuka ulang
        if (!rememberMe) {
            session.clearSession()
            session // runtime session akan hilang saat app ditutup
        }

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        replaceFragment(HomeFragment()) // default fragment

        // Tampilkan hanya label untuk item yang dipilih
        bottomNav.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_SELECTED

        // Atur skala awal (bikin default icon aktif sedikit lebih besar)
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

        bottomNav.setOnItemSelectedListener { item ->
            // animate selected icon
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

            // animate previous icon back to normal
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

            // ganti fragment sesuai item
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_jadwal -> replaceFragment(JadwalFragment())
                R.id.nav_password -> replaceFragment(PasswordFragment())
            }

            lastSelectedItemId = item.itemId
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
