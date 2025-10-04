package id.my.matahati.absensi


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        if (session.isLoggedIn()) {
            // ✅ langsung masuk ke HalamanScan
            val intent = Intent(this, HalamanScan::class.java)
            startActivity(intent)
            finish()
        } else {
            // ✅ kalau belum login, arahkan ke HalamanLogin
            val intent = Intent(this, HalamanLogin::class.java)
            startActivity(intent)
            finish()
        }
    }
}
