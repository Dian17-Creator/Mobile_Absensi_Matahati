package id.my.matahati.absensi


import android.app.Application
import androidx.room.Room
import id.my.matahati.absensi.data.AppDatabase

class MyApp : Application() {

    companion object {
        // database global, bisa dipakai di mana saja
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // inisialisasi Room database
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "myapp_db" // nama file database
        )
            .fallbackToDestructiveMigration() // hapus data kalau ada konflik versi DB
            .build()
    }
}
