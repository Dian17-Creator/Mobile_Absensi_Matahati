package id.my.matahati.absensi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import id.my.absensi.data.OfflineManualAbsenDao
import id.my.matahati.absensi.data.OfflineIzinDao

@Database(
    entities = [
        OfflineScan::class,
        OfflineManualAbsen::class,
        OfflineIzin::class,
        UserSchedule::class,
        AbsensiLog::class
    ],
    version = 9, // ⬆️ pastikan versi naik setiap menambah entity baru
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // ✅ Semua DAO yang tersedia
    abstract fun offlineScanDao(): OfflineScanDao
    abstract fun offlineManualAbsenDao(): OfflineManualAbsenDao
    abstract fun offlineIzinDao(): OfflineIzinDao
    abstract fun userScheduleDao(): UserScheduleDao
    abstract fun absensiLogDao(): AbsensiLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration() // ✅ otomatis rebuild DB jika ada tabel baru
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 🧩 (Opsional) Contoh migrasi lama yang bisa kamu hapus kalau tak dipakai
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE offline_scans ADD COLUMN status TEXT NOT NULL DEFAULT 'Pending'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_schedule (
                        nid INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nuserid INTEGER NOT NULL,
                        dwork TEXT NOT NULL,
                        dstart TEXT NOT NULL,
                        dend TEXT NOT NULL,
                        nidsched INTEGER NOT NULL,
                        cschedname TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
