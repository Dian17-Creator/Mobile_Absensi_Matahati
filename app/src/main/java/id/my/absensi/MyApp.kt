package id.my.matahati.absensi


import android.app.Application
import androidx.room.Room
import com.google.android.datatransport.BuildConfig
import id.my.matahati.absensi.data.AppDatabase
import java.io.File

class MyApp : Application() {

    companion object {
        // database global, bisa dipakai di mana saja
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("SESSION_DEBUG", "🔥 MyApp.onCreate() is running")

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "myapp_db"
        ).fallbackToDestructiveMigration().build()

        clearOldCache()
        clearSessionIfAppUpdated()
    }


    private fun clearSessionIfAppUpdated() {
        val prefs = getSharedPreferences("app_version_prefs", MODE_PRIVATE)
        val savedVersion = prefs.getInt("last_version_code", -1)
        val currentVersion = BuildConfig.VERSION_CODE

        if (savedVersion != currentVersion) {
            // 🔹 Hapus semua session lama (user_session)
            val sessionPrefs = getSharedPreferences("user_session", MODE_PRIVATE)
            sessionPrefs.edit().clear().apply()

            // 🔹 (Opsional) hapus juga prefs lain jika kamu pakai
            val otherPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            otherPrefs.edit().clear().apply()

            // 🔹 Simpan versi terbaru
            prefs.edit().putInt("last_version_code", currentVersion).apply()

            android.util.Log.d("SESSION_DEBUG", "App updated — old session cleared ✅")
        } else {
            android.util.Log.d("SESSION_DEBUG", "App version same — keep session")
        }
    }


    private fun clearOldCache() {
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
