package id.my.matahati.absensi

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_REMEMBER_ME = "rememberMe"
    }

    // ✅ Simpan data login (tergantung checkbox "ingatkan saya")
    fun login(id: Int, name: String, email: String, rememberMe: Boolean) {
        val editor = prefs.edit()
        editor.putInt(KEY_ID, id)
        editor.putString(KEY_NAME, name)
        editor.putString(KEY_EMAIL, email)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe)
        editor.apply()
    }

    // ✅ Ambil user ID langsung (kompatibel dengan file lama)
    fun getUserId(): Int = prefs.getInt(KEY_ID, -1)

    // ✅ Ambil semua data user
    fun getUser(): Map<String, Any?> = mapOf(
        "id" to prefs.getInt(KEY_ID, -1),
        "name" to prefs.getString(KEY_NAME, null),
        "email" to prefs.getString(KEY_EMAIL, null)
    )

    // ✅ Cek apakah sedang login
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    // ✅ Cek apakah “ingatkan saya” aktif
    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    // ✅ Hapus semua session (logout)
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
