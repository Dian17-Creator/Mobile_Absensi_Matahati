package id.my.matahati.absensi

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import id.my.matahati.absensi.BuildConfig
import id.my.matahati.absensi.data.AppDatabase
import java.io.File

class MyApp : Application(), Configuration.Provider {

    companion object {
        lateinit var db: AppDatabase
            private set
    }
    override fun onCreate() {
        super.onCreate()
        Log.d("SESSION_DEBUG", "🔥 MyApp.onCreate() is running")

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "myapp_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        try {
            WorkManager.initialize(
                this,
                Configuration.Builder()
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build()
            )
            Log.d("WorkManager", "✅ WorkManager initialized successfully")
        } catch (e: Exception) {
            Log.e("WorkManager", "❌ Gagal inisialisasi WorkManager: ${e.message}", e)
        }

        clearOldCache()
        clearSessionIfAppUpdated()
    }
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }
    private fun clearSessionIfAppUpdated() {
        val prefs = getSharedPreferences("app_version_prefs", MODE_PRIVATE)
        val savedVersion = prefs.getInt("last_version_code", -1)
        val currentVersion = BuildConfig.VERSION_CODE

        if (savedVersion != currentVersion) {

            val sessionPrefs = getSharedPreferences("user_session", MODE_PRIVATE)
            sessionPrefs.edit().clear().apply()
            val otherPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            otherPrefs.edit().clear().apply()
            prefs.edit().putInt("last_version_code", currentVersion).apply()
            Log.d("SESSION_DEBUG", "App updated — old session cleared ✅")

        } else {
            Log.d("SESSION_DEBUG", "App version same — keep session")
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
