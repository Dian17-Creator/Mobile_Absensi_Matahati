package id.my.matahati.absensi

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_REMEMBER_ME = "rememberMe"
    }

    // ✅ Simpan data user
    fun saveUser(id: Int, name: String, email: String) {
        prefs.edit().apply {
            putInt(KEY_ID, id)
            putString(KEY_NAME, name)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    // ✅ Ambil user ID
    fun getUserId(): Int = prefs.getInt(KEY_ID, -1)

    // ✅ Ambil semua data user
    fun getUser(): Map<String, Any?> = mapOf(
        "id" to prefs.getInt(KEY_ID, -1),
        "name" to prefs.getString(KEY_NAME, null),
        "email" to prefs.getString(KEY_EMAIL, null)
    )

    // ✅ Cek apakah user sudah login
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    // ✅ Hapus seluruh session (untuk logout)
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    // ✅ Simpan status “ingatkan saya”
    fun setRememberMe(value: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, value).apply()
    }

    // ✅ Ambil status “ingatkan saya”
    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)
}
