package id.my.matahati.absensi

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    // Simpan user
    fun saveUser(id: Int, name: String, email: String) {
        prefs.edit().apply {
            putInt("id", id)
            putString("name", name)
            putString("email", email)
            putBoolean("isLoggedIn", true)
            apply()
        }
    }

    // Ambil userId langsung
    fun getUserId(): Int {
        return prefs.getInt("id", -1)
    }

    // Ambil semua data user
    fun getUser(): Map<String, Any?> {
        return mapOf(
            "id" to prefs.getInt("id", -1),
            "name" to prefs.getString("name", null),
            "email" to prefs.getString("email", null)
        )
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("isLoggedIn", false)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
