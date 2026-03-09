package id.my.matahati.absensi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import id.my.matahati.absensi.data.UserAgenda
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAgendaDao {

    @Query("""
        SELECT * FROM agenda
        WHERE status = 'active'
        ORDER BY startAt
    """)
    fun getAgendas(): Flow<List<UserAgenda>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<UserAgenda>)

    @Query("DELETE FROM agenda")
    suspend fun clear()
}
