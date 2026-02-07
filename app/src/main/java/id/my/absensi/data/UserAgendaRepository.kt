package id.my.matahati.absensi.data

import android.content.Context
import id.my.matahati.absensi.data.ApiService
import id.my.matahati.absensi.data.UserAgenda
import id.my.matahati.absensi.data.UserAgendaDao
import id.my.matahati.absensi.data.ApiClient
import kotlinx.coroutines.flow.Flow

class UserAgendaRepository(context: Context) {

    private val dao =
        AppDatabase.getDatabase(context).userAgendaDao()

    private val api =
        ApiClient.apiService

    fun getLocalAgenda() = dao.getAgendas()

    suspend fun syncAgenda(month: String) {
        try {
            val result = api.getAgenda(month)

            dao.clear()
            dao.insertAll(result)

        } catch (e: Exception) {
            e.printStackTrace()
            // jangan crash
        }
    }

}


