package id.my.matahati.absensi


import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity

object LogoutHelper {
    fun logout(context: Context) {
        val session = SessionManager(context)
        session.clearSession()

        val intent = Intent(context, HalamanLogin::class.java)
        context.startActivity(intent)

        if (context is ComponentActivity) {
            context.finish()
        }
    }
}
