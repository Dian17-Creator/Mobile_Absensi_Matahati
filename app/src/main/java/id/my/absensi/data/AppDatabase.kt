package id.my.matahati.absensi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(
    entities = [OfflineScan::class, UserSchedule::class], // tambahkan UserSchedule
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offlineScanDao(): OfflineScanDao
    abstract fun userScheduleDao(): UserScheduleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // pakai semua migration
                    //.fallbackToDestructiveMigration() // aktifkan saat development kalau mau hapus DB lama
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration dari versi 1 -> 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE offline_scans ADD COLUMN status TEXT NOT NULL DEFAULT 'Pending'"
                )
            }
        }

        // Migration dari versi 2 -> 3
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
